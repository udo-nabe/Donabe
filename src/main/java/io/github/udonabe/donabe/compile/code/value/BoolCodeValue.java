package io.github.udonabe.donabe.compile.code.value;

public record BoolCodeValue(boolean value) implements CodeValue {
    
    @Override
    public byte type() {
        return CodeValue.BOOL_TYPE;
    }

    @Override
    public byte[] content() {
        return value
                ? new byte[]{0x01}
                : new byte[]{0x00};
    }
}
