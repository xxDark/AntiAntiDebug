package me.xdark.antiantidebug;

import me.coley.recaf.control.Controller;
import me.coley.recaf.plugin.api.StartupPlugin;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.VMUtil;
import me.coley.recaf.workspace.InstrumentationResource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.plugface.core.annotations.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.Map.Entry;

@Plugin(name = "AntiAntiDebug")
public final class AntiAntiDebugPlugin implements StartupPlugin {

    private static final boolean DEBUG = false;
    private static final String NATIVES = "me.xdark.antiantidebug.Natives";
    private static final String INTERNALS = "me.xdark.antiantidebug.InternalsUtil";
    private static final String PERF_DATA_FLAG = "-XX:-UsePerfData";
    private static final String ATTACH_FLAG = "-XX:+DisableAttachMechanism";
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static boolean nativeHooksSet;

    // Record VM structs as soon as possible.
    static {
        if (InstrumentationResource.isActive()) {
            VMStructs.init();
        }
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
            patchArgumentsList(args);
            String[] array = args.toArray(EMPTY_STRING_ARRAY);
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
            Field field = bean.getClass().getDeclaredField("jvm");
            field.setAccessible(true);
            Object jvm = field.get(bean);
            Class<?> jvmClass = jvm.getClass();
            field = jvmClass.getDeclaredField("vmArgs");
            field.setAccessible(true);
            field.set(jvm, Arrays.asList(array));
            redefineClass(instrumentation, jvmClass, (owner, method) -> {
                if ("getVmArguments0".equals(method.name) && "()[Ljava/lang/String;".equals(method.desc)) {
                    Log.trace("Transforming VMManagementImpl#getVmArguments0()");
                    method.access &= ~Opcodes.ACC_NATIVE;
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
                    method.instructions = inject;
                    return true;
                } else if ("getVerboseClass".equals(method.name) && "()Z".equals(method.desc)) {
                    Log.trace("Transforming VMManagementImpl#getVerboseClass()");
                    method.access &= ~Opcodes.ACC_NATIVE;
                    InsnList inject = new InsnList();
                    inject.add(new InsnNode(Opcodes.ICONST_0));
                    inject.add(new InsnNode(Opcodes.IRETURN));
                    method.instructions = inject;
                    return true;
                }
                return false;
            });
        } catch (Throwable t) {
            Log.error(t, "Unable to change runtime flags!");
        }
    }

    private static void hideAttachThread() {
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("Attach Listener".equals(t.getName())) {
                    if (nativeHooksSet) {
                        setNativeField("attachThread", t);
                    }
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

    private static void patchSystemProperties() {
        try {
            String cp = System.getProperty("java.class.path");
            File file = getRecafLocation();
            System.setProperty("java.class.path", patchClassPath(cp, File.pathSeparator, file));
        } catch (Throwable t) {
            Log.error(t, "Unable to patch system properties!");
        }
    }

    private static void patchVM() {
        String className = VMUtil.getVmVersion() > 8 ? "jdk.internal.misc.VM" : "sun.misc.VM";
        try {
            Class<?> vmClass = Class.forName(className, true, null);
            Field field = vmClass.getDeclaredField("savedProps");
            field.setAccessible(true);
            Properties properties = (Properties) field.get(null);
            String cp = properties.getProperty("java.class.path");
            File file = getRecafLocation();
            properties.setProperty("java.class.path", patchClassPath(cp, File.pathSeparator, file));
        } catch (Throwable t) {
            Log.error(t, "Unable to patch {}", className);
        }
    }

    private static void patchVMSupport(Instrumentation instrumentation) {
        String className =
                VMUtil.getVmVersion() > 8 ? "jdk.internal.misc.VMSupport" : "sun.misc.VMSupport";
        try {
            Class<?> supportClass = Class.forName(className, true, null);
            Method m = supportClass.getDeclaredMethod("initAgentProperties", Properties.class);
            m.setAccessible(true);
            Properties properties = (Properties) m.invoke(null, new Properties());
            String jvmArgs = properties.getProperty("sun.jvm.args");
            List<String> args = new ArrayList<>(Arrays.asList(jvmArgs.split(" ")));
            patchArgumentsList(args);
            properties.setProperty("sun.jvm.args", String.join(" ", args));
            Field field = supportClass.getDeclaredField("agentProps");
            field.setAccessible(true);
            field.set(null, properties);
            redefineClass(instrumentation, supportClass, (owner, method) -> {
                if ("initAgentProperties".equals(method.name)
                        && "(Ljava/util/Properties;)Ljava/util/Properties;".equals(method.desc)) {
                    Log.trace("Transforming VMSupport#initAgentProperties(Properties)");
                    method.access &= ~Opcodes.ACC_NATIVE;
                    InsnList inject = new InsnList();
                    inject.add(new TypeInsnNode(Opcodes.NEW, "java/util/Properties"));
                    inject.add(new InsnNode(Opcodes.DUP));
                    inject.add(
                            new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Properties", "<init>", "()V",
                                    false));
                    for (Entry<Object, Object> entry : properties.entrySet()) {
                        inject.add(new InsnNode(Opcodes.DUP));
                        inject.add(new LdcInsnNode(entry.getKey()));
                        inject.add(new LdcInsnNode(entry.getValue()));
                        inject.add(
                                new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Properties", "setProperty",
                                        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false));
                        inject.add(new InsnNode(Opcodes.POP));
                    }
                    inject.add(new InsnNode(Opcodes.ARETURN));
                    method.instructions = inject;
                    return true;
                }
                return false;
            });
        } catch (Throwable t) {
            Log.error(t, "Unable to patch {}", className);
        }
    }

    private static void setNativeHooks(Instrumentation instrumentation) {
        try {
            injectBootstrapClasses();
            setNativeField("instrumentation", instrumentation);
            patchThreads(instrumentation);
            nativeHooksSet = true;
        } catch (Throwable t) {
            Log.error(t, "Unable to set native hooks!");
        }
    }

    private static void patchThreads(Instrumentation instrumentation) {
        try {
            Class<?> threadClass = Thread.class;
            ClassNodeWrapper node = getBootstrapNode(threadClass.getName());
            setNativeField("originalThreadBytecode", node.code);
            if (redefineClass(instrumentation, node, threadClass, (owner, method) -> {
                if ("getThreads".equals(method.name) && "()[Ljava/lang/Thread;".equals(method.desc)) {
                    Log.trace("Transforming Thread#getThreads()");
                    method.access &= ~Opcodes.ACC_NATIVE;
                    InsnList inject = new InsnList();
                    inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, internalName(NATIVES),
                            "getThreads", "()[Ljava/lang/Thread;", false));
                    inject.add(new InsnNode(Opcodes.ARETURN));
                    method.instructions = inject;
                    return true;
                }
                return false;
            })) {
                setNativeField("rewrittenThreadBytecode", node.code);
            }
        } catch (Throwable t) {
            Log.error(t, "Unable to patch Thread#getThreads()!");
        }
    }

    private static boolean redefineClass(Instrumentation instrumentation, ClassNodeWrapper node,
                                         Class<?> klass,
                                         MethodPatcher methodPatcher)
            throws UnmodifiableClassException, ClassNotFoundException {
        boolean redefine = false;
        for (MethodNode mn : node.methods) {
            redefine |= methodPatcher.patch(node, mn);
        }
        if (redefine) {
            byte[] bytecode = ClassUtil.toCode(node, ClassWriter.COMPUTE_FRAMES);
            instrumentation.redefineClasses(new ClassDefinition(klass, bytecode));
            Natives.resetRedefineCount(klass);
            node.code = bytecode;
        }
        return redefine;
    }

    private static boolean redefineClass(Instrumentation instrumentation, Class<?> klass,
                                         MethodPatcher methodPatcher)
            throws UnmodifiableClassException, ClassNotFoundException {
        return redefineClass(instrumentation, getBootstrapNode(klass.getName()), klass, methodPatcher);
    }

    private static String patchClassPath(String cp, String separator, File toRemove) {
        for (String entry : cp.split(separator)) {
            File location = new File(entry);
            if (toRemove.equals(location)) {
                cp = cp.replace(separator + entry, "");
            }
        }
        return cp;
    }

    private static File getRecafLocation() throws URISyntaxException {
        return new File(StartupPlugin.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
    }

    private static void patchArgumentsList(List<String> args) {
        boolean foundDisableAttachMechanism = false;
        boolean foundDisablePerfData = false;
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith("-javaagent:")
                    || arg.startsWith("-agentlib:")
                    || arg.startsWith("-agentpath:")
                    || arg.startsWith("-verbose")
                    || arg.startsWith("-Xrun")
                    || arg.startsWith("-XX:+StartAttachListener")) {
                args.remove(i--);
            } else if (arg.startsWith(ATTACH_FLAG)) {
                foundDisableAttachMechanism = true;
            } else if (arg.startsWith(PERF_DATA_FLAG)) {
                foundDisablePerfData = true;
            }
        }
        if (!foundDisableAttachMechanism) {
            args.add(0, ATTACH_FLAG);
        }
        if (!foundDisablePerfData) {
            args.add(0, PERF_DATA_FLAG);
        }
    }

    private static ClassNodeWrapper getBootstrapNode(String name) {
        ClassReader reader = ClassUtil.fromRuntime(name);
        ClassNodeWrapper node = new ClassNodeWrapper();
        reader.accept(node, 0);
        node.code = reader.b;
        return node;
    }

    private static void injectBootstrapClasses()
            throws Exception {
        String unsafeClass = VMUtil.getVmVersion() > 8 ? "jdk.internal.misc.Unsafe" : "sun.misc.Unsafe";
        Class<?> klass = Class.forName(unsafeClass, true, null);
        Field field = klass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method define = klass
                .getDeclaredMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE,
                        ClassLoader.class,
                        ProtectionDomain.class);
        define.setAccessible(true);
        loadBootstrapClass(INTERNALS, unsafe, define);
        loadBootstrapClass(NATIVES, unsafe, define);
    }

    private static void loadBootstrapClass(String className, Object unsafe, Method define)
            throws IOException, InvocationTargetException, IllegalAccessException {
        try (InputStream in = AntiAntiDebugPlugin.class.getClassLoader()
                .getResourceAsStream(internalName(className) + ".class")) {
            if (in == null) {
                throw new RuntimeException("Cannot locate: " + className);
            }
            byte[] code = IOUtil.toByteArray(in);
            define.invoke(unsafe, null, code, 0, code.length, null, null);
        }
    }

    private static void setNativeField(String field, Object value) {
        try {
            Class<?> klass = Class.forName(NATIVES, true, null);
            Field f = klass.getDeclaredField(field);
            f.setAccessible(true);
            f.set(null, value);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String internalName(String name) {
        return name.replace('.', '/');
    }

    @Override
    public void onStart(Controller controller) {
        Instrumentation instrumentation = InstrumentationResource.instrumentation;
        if (instrumentation != null) {
            if (DEBUG) {
                try {
                    InstrumentationResource.getInstance().setSkippedPrefixes(Collections.emptyList());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            setNativeHooks(instrumentation);
            patchSystemPaths();
            patchVMManagement(instrumentation);
            hideAttachThread();
            patchSystemProperties();
            patchVM();
            patchVMSupport(instrumentation);
        }
    }

    @Override
    public String getVersion() {
        return "1.0.6";
    }

    @Override
    public String getDescription() {
        return "Prevents VMs from detecting Recaf";
    }
}
