package io.github.udonabe.donabe.runtime.value;

public final class VoidValue implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<void>";
    }

    @Override
    public String typeName() {
        return "void";
    }

    @Override
    public String display() {
        return "<void>";
    }
}
