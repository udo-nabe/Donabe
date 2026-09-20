package io.github.udonabe.donabe.compile.code.constant;

import java.nio.charset.StandardCharsets;

public record MethodRefEntry(String value) implements ConstantPoolEntry<String> {
    
    @Override
    public byte type() {
        return METHOD_REF_TYPE;
    }

    @Override
    public byte[] content() {
        return value.getBytes(StandardCharsets.UTF_8);
    }

}
