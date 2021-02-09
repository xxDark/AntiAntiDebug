package me.xdark.antiantidebug;

public final class WrappedClassLoader extends ClassLoader {

  public WrappedClassLoader(ClassLoader parent) {
    super(parent);
  }
}
