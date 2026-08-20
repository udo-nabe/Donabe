package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Objects;

public final class VariableCell {
    private RuntimeValue<?> value;

    public VariableCell(RuntimeValue<?> value) {
        this.value = value;
    }


    public RuntimeValue<?> value() {
        return value;
    }

    public void setValue(RuntimeValue<?> value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (VariableCell) obj;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "VariableValue[" +
               "value=" + value + ']';
    }
}
