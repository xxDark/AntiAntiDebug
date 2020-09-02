package me.xdark.antiantidebug;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

final class ClassNodeWrapper extends ClassNode {
  byte[] code;

  ClassNodeWrapper() {
    super(Opcodes.ASM8);
  }
}
