package io.github.udonabe.donabe.ir.value;

import java.util.Objects;

public record IntegerValue(Integer value) implements RuntimeValue<Integer> {
    public IntegerValue {
        Objects.requireNonNull(value);
    }
}
