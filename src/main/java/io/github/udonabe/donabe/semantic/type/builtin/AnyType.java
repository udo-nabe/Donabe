package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.Type;

public record AnyType() implements BuiltinType {
    @Override
    public String asString() {
        return "Any";
    }

    @Override
    public boolean isCompatible(Type target) {
        return true;
    }
}
