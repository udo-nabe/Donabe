package io.github.udonabe.donabe.compile.code.value;

import java.nio.charset.StandardCharsets;

public record StringCodeValue(String value) implements CodeValue {
    
    @Override
    public byte type() {
        return CodeValue.STRING_TYPE;
    }

    @Override
    public byte[] content() {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
