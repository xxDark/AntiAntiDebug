package me.xdark.antiantidebug;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

public final class InternalsUtil {

  private static final Unsafe UNSAFE;
  private static final Lookup LOOKUP;

  private InternalsUtil() {
  }

  public static Unsafe unsafe() {
    return UNSAFE;
  }

  public static Lookup lookup() {
    return LOOKUP;
  }

  static {
    try {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      Unsafe unsafe = UNSAFE = (Unsafe) field.get(null);
      field = Lookup.class.getDeclaredField("IMPL_LOOKUP");
      LOOKUP = (Lookup) unsafe
          .getObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
    } catch (IllegalAccessException | NoSuchFieldException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }
}
