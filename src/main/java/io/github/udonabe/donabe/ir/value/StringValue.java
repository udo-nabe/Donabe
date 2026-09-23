package io.github.udonabe.donabe.ir.value;

import java.util.Objects;

public record StringValue(String value) implements RuntimeValue<String> {
    public StringValue {
        Objects.requireNonNull(value);
    }
}
