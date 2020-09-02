package me.xdark.antiantidebug;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.VMUtil;

public final class NativeLibraryLoader {

  private static final Loader LOADER;

  private NativeLibraryLoader() {
  }

  public static NativeLibrary loadJvmLibrary() {
    Path jvmDir = Paths.get(System.getProperty("java.home"));
    Path maybeJre = jvmDir.resolve("jre");
    if (Files.isDirectory(maybeJre)) {
      jvmDir = maybeJre;
    }
    jvmDir = jvmDir.resolve("bin");
    String os = System.getProperty("os.name").toLowerCase();
    Path pathToJvm;
    if (os.contains("win")) {
      pathToJvm = findFirstFile(jvmDir, "server/jvm.dll", "client/jvm.dll");
    } else if (os.contains("nix") || os.contains("nux")) {
      pathToJvm = findFirstFile(jvmDir, "lib/amd64/server/libjvm.so", "lib/i386/server/libjvm.so");
    } else {
      throw new RuntimeException("Unsupported OS (probably MacOS X): " + os);
    }
    return loadLibrary(pathToJvm);
  }

  public static NativeLibrary loadLibrary(Path path) {
    try {
      return LOADER.loadLibrary(IOUtil.toString(path));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private static Path findFirstFile(Path directory, String... files) {
    for (String file : files) {
      Path path = directory.resolve(file);
      if (Files.exists(path)) {
        return path;
      }
    }
    throw new RuntimeException(
        "Failed to find one of the required paths!: " + Arrays.toString(files));
  }


  static {
    LOADER = VMUtil.getVmVersion() > 8 ? new Java9LibraryLoader() : new Java8LibraryLoader();
  }

  private static abstract class Loader {

    protected static final Class<?> CL_NATIVE_LIBRARY;
    protected static final MethodHandle CNSTR_NATIVE_LIBRARY;

    abstract NativeLibrary loadLibrary(String path) throws Throwable;

    static {
      try {
        CL_NATIVE_LIBRARY = Class.forName("java.lang.ClassLoader$NativeLibrary", true, null);
        CNSTR_NATIVE_LIBRARY = InternalsUtil.lookup().findConstructor(CL_NATIVE_LIBRARY,
            MethodType.methodType(Void.TYPE, Class.class, String.class, Boolean.TYPE));
      } catch (Throwable t) {
        throw new ExceptionInInitializerError(t);
      }
    }
  }

  private static class Java8LibraryLoader extends Loader {

    private static final MethodHandle MH_NATIVE_LOAD;
    private static final MethodHandle MH_NATIVE_FIND;
    private static final MethodHandle MH_NATIVE_LOADED_SET;
    private static final MethodHandle MH_NATIVE_UNLOAD;

    @Override
    NativeLibrary loadLibrary(String path) throws Throwable {
      Object library = CNSTR_NATIVE_LIBRARY.invoke(NativeLibraryLoader.class, path, false);
      MH_NATIVE_LOAD.invoke(library, path, false);
      MH_NATIVE_LOADED_SET.invoke(library, true);
      return new NativeLibrary() {

        @Override
        public long findEntry(String entry) {
          try {
            return (long) MH_NATIVE_FIND.invoke(library, entry);
          } catch (Throwable t) {
            throw new RuntimeException(t);
          }
        }

        @Override
        public void unload() {
          try {
            MH_NATIVE_UNLOAD.invoke(library, path, false);
            MH_NATIVE_LOADED_SET.invoke(library, false);
          } catch (Throwable t) {
            throw new RuntimeException(t);
          }
        }
      };
    }

    static {
      Lookup lookup = InternalsUtil.lookup();
      Class<?> cl = CL_NATIVE_LIBRARY;
      try {
        MH_NATIVE_LOAD = lookup
            .findVirtual(cl, "load", MethodType.methodType(Void.TYPE, String.class, Boolean.TYPE));
        MH_NATIVE_FIND = lookup
            .findVirtual(cl, "find", MethodType.methodType(Long.TYPE, String.class));
        MH_NATIVE_LOADED_SET = lookup.findSetter(cl, "loaded", Boolean.TYPE);
        MH_NATIVE_UNLOAD = lookup.findVirtual(cl, "unload",
            MethodType.methodType(Void.TYPE, String.class, Boolean.TYPE));
      } catch (Throwable t) {
        throw new ExceptionInInitializerError(t);
      }

    }
  }

  private static class Java9LibraryLoader extends Loader {

    private static final MethodHandle MH_NATIVE_LOAD;
    private static final MethodHandle MH_NATIVE_FIND;
    private static final MethodHandle MH_NATIVE_HANDLE;
    private static final MethodHandle MH_NATIVE_UNLOAD;

    @Override
    NativeLibrary loadLibrary(String path) throws Throwable {
      Object library = CNSTR_NATIVE_LIBRARY.invoke(NativeLibraryLoader.class, path, false);
      MH_NATIVE_LOAD.invoke(library, path, false);
      return new NativeLibrary() {
        @Override
        public long findEntry(String entry) {
          try {
            return (long) MH_NATIVE_FIND.invoke(library, entry);
          } catch (Throwable t) {
            throw new RuntimeException(t);
          }
        }

        @Override
        public void unload() {
          try {
            MH_NATIVE_UNLOAD.invoke(path, false, MH_NATIVE_HANDLE.invoke(library));
          } catch (Throwable t) {
            throw new RuntimeException(t);
          }
        }
      };
    }

    static {
      Lookup lookup = InternalsUtil.lookup();
      Class<?> cl = CL_NATIVE_LIBRARY;
      try {
        MH_NATIVE_LOAD = lookup.findVirtual(cl, "load0", MethodType
            .methodType(Boolean.TYPE, String.class, Boolean.TYPE));
        MH_NATIVE_FIND = lookup
            .findVirtual(cl, "findEntry", MethodType.methodType(Long.TYPE, String.class));
        MH_NATIVE_HANDLE = lookup.findGetter(cl, "handle", Long.TYPE);
        MH_NATIVE_UNLOAD = lookup.findStatic(cl, "unload",
            MethodType.methodType(Void.TYPE, String.class, Boolean.TYPE, Long.TYPE));
      } catch (Throwable t) {
        throw new ExceptionInInitializerError(t);
      }

    }
  }
}
