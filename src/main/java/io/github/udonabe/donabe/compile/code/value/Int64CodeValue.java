package io.github.udonabe.donabe.compile.code.value;

import io.github.udonabe.donabe.compile.code.EndianUtil;

public record Int64CodeValue(long value) implements CodeValue {
    
    @Override
    public byte type() {
        return CodeValue.INT64_TYPE;
    }

    @Override
    public byte[] content() {
        //リトルエンディアンで書き出す
        return EndianUtil.to8BytesLittleEndian(value);
    }
}
