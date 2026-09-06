package io.github.udonabe.donabe.compile.code.instruction.operand;

import io.github.udonabe.donabe.compile.code.EndianUtil;

public record ConstantPoolOperand(int value) implements Operand {

}
