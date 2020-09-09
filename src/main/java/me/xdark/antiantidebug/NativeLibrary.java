package me.xdark.antiantidebug;

public interface NativeLibrary {

    long findEntry(String entry);

    void unload();
}
