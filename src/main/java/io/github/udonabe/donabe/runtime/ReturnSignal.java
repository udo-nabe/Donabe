package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.runtime.value.RuntimeValue;

public class ReturnSignal extends RuntimeException {
    private final RuntimeValue<?> value;

    public ReturnSignal(RuntimeValue<?> value) {
        this.value = value;
    }

    public RuntimeValue<?> value() {
        return value;
    }
}
