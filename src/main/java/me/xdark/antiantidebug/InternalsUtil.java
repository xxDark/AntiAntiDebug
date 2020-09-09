package me.xdark.antiantidebug;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;

public final class InternalsUtil {

    private static final Unsafe UNSAFE;
    private static final Lookup LOOKUP;

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

    private InternalsUtil() {
    }

    public static Unsafe unsafe() {
        return UNSAFE;
    }

    public static Lookup lookup() {
        return LOOKUP;
    }
}
