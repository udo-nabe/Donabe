package io.github.udonabe.donabe.runtime.value;

import java.util.Objects;

public record StringValue(String value) implements RuntimeValue<String> {
    public StringValue {
        Objects.requireNonNull(value);
    }
    @Override
    public String typeName() {
        return "string";
    }

    @Override
    public String display() {
        return value;
    }
}
