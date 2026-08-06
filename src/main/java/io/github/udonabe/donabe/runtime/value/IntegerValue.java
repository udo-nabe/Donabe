package io.github.udonabe.donabe.runtime.value;

import java.util.Objects;

public record IntegerValue(Integer value) implements RuntimeValue<Integer> {
    public IntegerValue {
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
