package io.github.udonabe.donabe.compile.code.instruction.operand;

import io.github.udonabe.donabe.compile.code.EndianUtil;

public record SizeOperand(int value) implements Operand {

    @Override
    public byte[] content() {
        return EndianUtil.to4BytesLittleEndian(value);
    }
}
