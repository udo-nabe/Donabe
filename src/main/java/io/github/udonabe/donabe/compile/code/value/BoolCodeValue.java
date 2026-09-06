package io.github.udonabe.donabe.compile.code.value;

public record BoolCodeValue(boolean value) implements CodeValue {
    
    @Override
    public byte type() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte[] content() {
        return value
                ? new byte[]{1}
                : new byte[]{0};
    }
}
