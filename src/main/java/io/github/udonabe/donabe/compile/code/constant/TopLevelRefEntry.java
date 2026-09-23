package io.github.udonabe.donabe.compile.code.constant;

import java.nio.charset.StandardCharsets;

public record TopLevelRefEntry(String value) implements ConstantPoolEntry<String> {
    
    @Override
    public byte type() {
        return TOPLEVEL_REF_TYPE;
    }

    @Override
    public byte[] content() {
        return value.getBytes(StandardCharsets.UTF_8);
    }

}
