package io.github.udonabe.donabe.compile.code.instruction.operand;

import io.github.udonabe.donabe.compile.code.EndianUtil;

public interface Operand {

    int value();

    default byte[] content() {
        return EndianUtil.to4BytesLittleEndian(value());
    }
}
