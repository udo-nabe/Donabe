package io.github.udonabe.donabe.ir.value;

import java.util.Objects;

public record Int64Value(Long value) implements RuntimeValue<Long> {
    public Int64Value {
        Objects.requireNonNull(value);
    }
}
