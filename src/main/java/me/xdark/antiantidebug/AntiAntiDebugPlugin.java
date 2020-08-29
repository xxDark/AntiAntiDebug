package me.xdark.antiantidebug;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import me.coley.recaf.control.Controller;
import me.coley.recaf.plugin.api.StartupPlugin;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.struct.VMUtil;
import me.coley.recaf.workspace.InstrumentationResource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.plugface.core.annotations.Plugin;

@Plugin(name = "AntiAntiDebug")
public final class AntiAntiDebugPlugin implements StartupPlugin {

  @Override
  public void onStart(Controller controller) {
    Instrumentation instrumentation = InstrumentationResource.instrumentation;
    if (instrumentation != null) {
      patchSystemPaths();
      patchVMManagement(instrumentation);
      hideAttachThread();
    }
  }

  @Override
  public String getVersion() {
    return "1.0.2";
  }

  @Override
  public String getDescription() {
    return "Prevents VMs from detecting Recaf";
  }

  private static void patchSystemPaths() {
    try {
      ClassLoader loader = ClassLoader.getSystemClassLoader();
      URL url = StartupPlugin.class.getProtectionDomain().getCodeSource().getLocation();
      if (loader instanceof URLClassLoader) {
        Field field = URLClassLoader.class.getDeclaredField("ucp");
        field.setAccessible(true);
        Object ucp = field.get(loader);
        removeURL(ucp, url);
      } else {
        if (VMUtil.getVmVersion() > 8) {
          try {
            Field field = loader.getClass().getSuperclass().getDeclaredField("ucp");
            field.setAccessible(true);
            Object ucp = field.get(null);
            removeURL(ucp, url);
          } catch (NoSuchFieldException ignored) {
          }
        }
      }
    } catch (Throwable t) {
      Log.error(t, "Unable to patch system class path!");
    }
  }

  private static void removeURL(Object ucp, URL url) {
    try {
      Class<?> ucpClass = ucp.getClass();
      Field field = ucpClass.getDeclaredField("urls");
      field.setAccessible(true);
      ((Collection<URL>) field.get(ucp)).remove(url);
      field = ucpClass.getDeclaredField("path");
      field.setAccessible(true);
      ((Collection<URL>) field.get(ucp)).remove(url);
    } catch (Throwable t) {
      Log.error(t, "Unable to remove url from class path!");
    }
  }

  private static void patchVMManagement(Instrumentation instrumentation) {
    try {
      List<String> args = new ArrayList<>(
          ManagementFactory.getRuntimeMXBean().getInputArguments());
      boolean foundDisableAttachMechanism = false;
      boolean foundDisablePerfData = false;
      for (int i = 0; i < args.size(); i++) {
        String arg = args.get(i);
        if (arg.startsWith("-javaagent:")
            || arg.startsWith("-agentlib:")
            || arg.startsWith("-agentpath")
            || arg.startsWith("-verbose")) {
          args.remove(i--);
        } else if (arg.startsWith("-XX:+DisableAttachMechanism")) {
          foundDisableAttachMechanism = true;
        } else if (arg.startsWith("-XX:-UsePerfData")) {
          foundDisablePerfData = true;
        }
      }
      if (!foundDisableAttachMechanism) {
        args.add(0, "-XX:+DisableAttachMechanism");
      }
      if (!foundDisablePerfData) {
        args.add(0, "-XX:-UsePerfData");
      }
      RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
      Field field = bean.getClass().getDeclaredField("jvm");
      field.setAccessible(true);
      Object jvm = field.get(bean);
      Class<?> jvmClass = jvm.getClass();
      field = jvmClass.getDeclaredField("vmArgs");
      field.setAccessible(true);
      field.set(jvm, Arrays.asList(args.toArray(new String[0])));
      ClassReader reader = ClassUtil.fromRuntime(jvmClass.getName());
      ClassNode node = ClassUtil.getNode(reader, 0);
      boolean redefine = false;
      for (MethodNode mn : node.methods) {
        if ("getVmArguments0".equals(mn.name) && "()[Ljava/lang/String;".equals(mn.desc)) {
          redefine = true;
          Log.trace("Transforming VMManagementImpl#getVmArguments0()");
          mn.access &= ~Opcodes.ACC_NATIVE;
          InsnList inject = new InsnList();
          inject.add(new LdcInsnNode(args.size()));
          inject.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
          for (int i = 0, j = args.size(); i < j; i++) {
            inject.add(new InsnNode(Opcodes.DUP));
            inject.add(new LdcInsnNode(i));
            inject.add(new LdcInsnNode(args.get(i)));
            inject.add(new InsnNode(Opcodes.AASTORE));
          }
          inject.add(new InsnNode(Opcodes.ARETURN));
          mn.instructions = inject;
        } else if ("getVerboseClass".equals(mn.name) && "()Z".equals(mn.desc)) {
          redefine = true;
          Log.trace("Transforming VMManagementImpl#getVerboseClass()");
          mn.access &= ~Opcodes.ACC_NATIVE;
          InsnList inject = new InsnList();
          inject.add(new InsnNode(Opcodes.ICONST_0));
          inject.add(new InsnNode(Opcodes.IRETURN));
          mn.instructions = inject;
        }
      }
      if (redefine) {
        byte[] bytecode = ClassUtil.toCode(node, ClassWriter.COMPUTE_FRAMES);
        instrumentation.redefineClasses(new ClassDefinition(jvmClass, bytecode));
      }
    } catch (Throwable t) {
      Log.error(t, "Unable to change runtime flags!");
    }
  }

  private static void hideAttachThread() {
    try {
      for (Thread t : Thread.getAllStackTraces().keySet()) {
        if ("Attach Listener".equals(t.getName())) {
          t.setName("main");
          Method m = Thread.class.getDeclaredMethod("setNativeName", String.class);
          m.setAccessible(true);
          m.invoke(t, "main");
          break;
        }
      }
    } catch (Throwable t) {
      Log.error(t, "Unable to 'hide' Attach Listener thread!");
    }
  }
}
