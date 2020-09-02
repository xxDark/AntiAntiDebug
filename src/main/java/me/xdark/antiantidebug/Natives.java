package me.xdark.antiantidebug;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.Arrays;
import me.coley.recaf.util.Log;

public final class Natives {

  private static final Object THREAD_LOCK = new Object();
  private static final MethodHandle MH_THREADS;
  private static final MethodHandle MH_SET_REDEFINE_COUNT;
  private static final MethodHandle MH_SET_REFLECTION_DATA;
  private static final MethodHandle MH_THREAD_REGISTER_NATIVES;
  static Instrumentation instrumentation;
  static byte[] originalThreadBytecode;
  static byte[] rewrittenThreadBytecode;
  static Thread attachThread;

  private Natives() {
  }

  public static Thread[] getThreads() {
    synchronized (THREAD_LOCK) {
      try {
        Class<?> threadClass = Thread.class;
        Instrumentation instrumentation = Natives.instrumentation;
        instrumentation.redefineClasses(new ClassDefinition(threadClass, originalThreadBytecode));
        resetRedefineCount(threadClass);
        MH_THREAD_REGISTER_NATIVES.invokeExact();
        Thread[] threads = (Thread[]) MH_THREADS.invokeExact();
        Thread t1 = attachThread;
        if (t1 != null) {
          threads = Arrays.stream(threads).filter(t -> t != t1).toArray(Thread[]::new);
        }
        instrumentation.redefineClasses(new ClassDefinition(threadClass, rewrittenThreadBytecode));
        resetRedefineCount(threadClass);
        return threads;
      } catch (Throwable t) {
        throw new InternalError(t);
      }
    }
  }

  static void resetRedefineCount(Class<?> klass) {
    try {
      MH_SET_REDEFINE_COUNT.invokeExact(klass, 0);
      MH_SET_REFLECTION_DATA.invokeExact(klass, (SoftReference) null);
    } catch (Throwable t) {
      Log.error(t, "Failed to reset redefine count for: {}", klass);
    }
  }

  static {
    try {
      Field field = Lookup.class.getDeclaredField("IMPL_LOOKUP");
      field.setAccessible(true);
      MethodHandles.publicLookup();
      Lookup lookup = (Lookup) field.get(null);
      MH_THREADS = lookup
          .findStatic(Thread.class, "getThreads", MethodType.methodType(Thread[].class));
      MH_SET_REDEFINE_COUNT = lookup.findSetter(Class.class, "classRedefinedCount", Integer.TYPE);
      MH_SET_REFLECTION_DATA = lookup
          .findSetter(Class.class, "reflectionData", SoftReference.class);
      MH_THREAD_REGISTER_NATIVES = lookup
          .findStatic(Thread.class, "registerNatives", MethodType.methodType(Void.TYPE));
    } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }
}
