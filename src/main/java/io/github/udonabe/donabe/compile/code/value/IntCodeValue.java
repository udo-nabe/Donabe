package io.github.udonabe.donabe.compile.code.value;

import io.github.udonabe.donabe.compile.code.EndianUtil;

public record IntCodeValue(int value) implements CodeValue {
    
    @Override
    public byte type() {
        return CodeValue.INT_TYPE;
    }

    @Override
    public byte[] content() {
        //リトルエンディアンで書き出す
        return EndianUtil.to4BytesLittleEndian(value);
    }
}
