package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.InterpreterException;

import java.util.Map;
import java.util.Objects;

public record IntegerValue(Integer value) implements RuntimeValue<Integer> {
    public IntegerValue {
        Objects.requireNonNull(value);
    }
    @Override
    public String typeName() {
        return "Int";
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
