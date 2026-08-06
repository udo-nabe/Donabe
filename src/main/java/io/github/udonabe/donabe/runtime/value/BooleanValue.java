package io.github.udonabe.donabe.runtime.value;

import java.util.Objects;

public record BooleanValue(Boolean value) implements RuntimeValue<Boolean> {
    public BooleanValue {
        Objects.requireNonNull(value);
    }

    @Override
    public String typeName() {
        return "int";
    }

    @Override
    public String display() {
        return value.toString();
    }
}
