package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.InterpreterException;

import java.util.Map;
import java.util.Objects;

public record BooleanValue(Boolean value) implements RuntimeValue<Boolean> {
    public BooleanValue {
        Objects.requireNonNull(value);
    }

    @Override
    public String typeName() {
        return "Bool";
    }

    @Override
    public String display() {
        return value.toString();
    }

    @Override
    public Map<String, RuntimeValue<?>> declaredMembers() {
        return Map.of();
    }
}
