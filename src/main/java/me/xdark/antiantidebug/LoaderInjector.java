package me.xdark.antiantidebug;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import jdk.internal.org.objectweb.asm.ClassReader;
import sun.instrument.InstrumentationImpl;
import sun.instrument.TransformerManager;
import sun.misc.Unsafe;

/**
 * One of the techniques of detecting/preventing attach API
 * involves an attempt to load in instrumentation classes
 *
 * If class loading is unsuccessful, agent is probably attached.
 * Well, not in our case.
 */
public final class LoaderInjector {

  private static final Set<String> CLASSES = new HashSet<>(
      Arrays.asList(internalName(Instrumentation.class),
          internalName(InstrumentationImpl.class),
          internalName(TransformerManager.class),
          internalName(ClassDefinition.class),
          internalName(ClassFileTransformer.class))
  );
  private static final Unsafe UNSAFE = InternalsUtil.unsafe();
  private static final ClassLoader BOOT_LOADER = new WrappedClassLoader(null);
  private static final Map<ClassLoader, ClassLoader> LOADERS = Collections
      .synchronizedMap(new WeakHashMap<>());
  private static final MethodHandle MH_DEFINE_CLASS_1;
  private static final MethodHandle MH_SET_LOADER;

  private LoaderInjector() {
  }

  public static Class<?> defineClass(ClassLoader loader, String name, byte[] classBytes, int off,
      int len, ProtectionDomain domain) {
    return defineClass(loader, name, classBytes, off, len, domain, "JVM_DefineClass");
  }

  // to match Unsafe descriptor
  public static Class<?> defineClass(String name, byte[] b, int off, int len,
      ClassLoader loader,
      ProtectionDomain protectionDomain) {
    return defineClass(loader, name, b, off, len, protectionDomain, "JVM_DefineClass");
  }

  public static Class<?> defineClass(ClassLoader loader, String name, byte[] classBytes, int off,
      int len, ProtectionDomain domain, String source) {
    try {
      return defineClassImpl(loader, name, classBytes, off, len, domain, source);
    } catch (Throwable t) {
      if (!(loader instanceof WrappedClassLoader)) {
        // :)
        String internalName = getClassName(name, classBytes, off, len);
        if (CLASSES.contains(internalName)) {
          ClassLoader fake = getLoader(loader);
          Class<?> fooled;
          try {
            fooled = defineClassImpl(fake, name, classBytes, off, len, domain, source);
          } catch (Throwable t1) {
            // throw original throwable
            throwException(t);
            return null;
          }
          try {
            MH_SET_LOADER.invokeExact(fooled, loader);
          } catch (Throwable t1) {
            t1.printStackTrace(); // TODO handle that better
          }
          return fooled;
        }
      }
      throwException(t);
      return null;
    }
  }

  public static Class<?> defineClass(ClassLoader loader, String name, ByteBuffer buffer,
      ProtectionDomain domain, String source) {
    byte[] bytes = getBufferBytes(buffer);
    return defineClass(loader, name, bytes, 0, bytes.length, domain, source);
  }

  private static Class<?> defineClassImpl(ClassLoader loader, String name, byte[] classBytes,
      int off,
      int len, ProtectionDomain domain, String source) {
    // Special case for null loader
    // We lose source string, but who cares?
    if (loader == null) {
      return UNSAFE.defineClass(name, classBytes, off, len, null, domain);
    }
    try {
      return (Class<?>) MH_DEFINE_CLASS_1.invokeExact(loader, name, classBytes, off, len, domain, source);
    } catch (Throwable t) {
      throwException(t);
      return null;
    }
  }

  private static ClassLoader getLoader(ClassLoader loader) {
    return loader == null ? BOOT_LOADER : LOADERS.computeIfAbsent(loader, WrappedClassLoader::new);
  }

  private static String getClassName(String maybe, byte[] classBytes, int off, int len) {
    return maybe != null ? internalName(maybe)
        : new ClassReader(classBytes, off, len).getClassName();
  }

  private static byte[] getBufferBytes(ByteBuffer buffer) {
    if (buffer.hasArray()) {
      int off = buffer.arrayOffset(), len = buffer.limit();
      byte[] array = buffer.array();
      return off == 0 && len == array.length ? array : Arrays.copyOfRange(array, off, len);
    }
    byte[] bytes = new byte[buffer.remaining()];
    int position = buffer.position();
    buffer.get(bytes).position(position);
    return bytes;
  }

  private static String internalName(Class<?> c) {
    return c.getName().replace('.', '/');
  }

  private static String internalName(String name) {
    return name.replace('.', '/');
  }

  private static void throwException(Throwable t) {
    UNSAFE.throwException(t);
  }

  static {
    try {
      Lookup lookup = InternalsUtil.lookup();
      MH_DEFINE_CLASS_1 = lookup
          .findVirtual(ClassLoader.class, "defineClass1", MethodType.methodType(
              Class.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class,
              String.class));
      MH_SET_LOADER = lookup.findSetter(Class.class, "classLoader", ClassLoader.class);
    } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }
}
