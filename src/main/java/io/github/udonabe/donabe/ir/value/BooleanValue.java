package io.github.udonabe.donabe.ir.value;


import java.util.Objects;

public record BooleanValue(Boolean value) implements RuntimeValue<Boolean> {
    public BooleanValue {
        Objects.requireNonNull(value);
    }
}
