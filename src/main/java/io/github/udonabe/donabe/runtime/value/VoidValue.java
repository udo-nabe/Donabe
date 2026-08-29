package io.github.udonabe.donabe.runtime.value;

public final class VoidValue implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<void>";
    }

    @Override
    public String typeName() {
        return "Void";
    }

    @Override
    public String display() {
        return "<void>";
    }

    @Override
    public boolean equals(Object obj) {
        return true;
    }

    @Override
    public RuntimeValue<?> getMember(String name) {
        throw new IllegalStateException("Cannot use 'Void' type value.");
    }
}
