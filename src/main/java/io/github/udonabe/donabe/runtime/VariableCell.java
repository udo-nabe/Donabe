package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Objects;

public final class VariableCell {
    private final boolean mutable;
    private RuntimeValue<?> value;
    private boolean temporary;

    public VariableCell(boolean mutable, RuntimeValue<?> value, boolean temporary) {
        this.mutable = mutable;
        this.value = value;
        this.temporary = temporary;
    }

    public VariableCell(boolean mutable, RuntimeValue<?> value) {
        this(mutable, value, false);
    }

    public boolean mutable() {
        return mutable;
    }

    public RuntimeValue<?> value() {
        return value;
    }

    public void setValue(RuntimeValue<?> value) {
        this.value = value;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (VariableCell) obj;
        return this.mutable == that.mutable &&
               Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mutable, value);
    }

    @Override
    public String toString() {
        return "VariableValue[" +
               "mutable=" + mutable + ", " +
               "value=" + value + ']';
    }

    public boolean temporary() {
        return temporary;
    }
}
