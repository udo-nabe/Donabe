package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.Type;

public record VoidType() implements BuiltinType {
    @Override
    public String asString() {
        return "Void";
    }

    @Override
    public boolean isCompatible(Type target) {
        return equals(target) || target instanceof AnyType;
    }
}
