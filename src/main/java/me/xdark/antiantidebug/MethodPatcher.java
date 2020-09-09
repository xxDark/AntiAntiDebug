package me.xdark.antiantidebug;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@FunctionalInterface
public interface MethodPatcher {

    boolean patch(ClassNode owner, MethodNode method);
}
