package io.github.udonabe.donabe.compile.code.instruction.operand;

import io.github.udonabe.donabe.compile.code.EndianUtil;

public record DepthOperand(int value) implements Operand {

    public DepthOperand {
        if (value > 65535 || value < 0) {
            throw new IllegalArgumentException("The argument 'value' must be a range of u16.");
        }
    }

    @Override
    public byte[] content() {
        return EndianUtil.to2BytesLittleEndian(value);
    }
}
