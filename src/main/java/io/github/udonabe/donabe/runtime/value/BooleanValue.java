package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.InterpreterException;

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
    public RuntimeValue<?> getMember(String name) {
        throw new InterpreterException("The type 'Bool' does not have nothing members.");
    }
}
