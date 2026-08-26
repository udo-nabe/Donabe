package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.Type;

public record IntType() implements BuiltinType {
    @Override
    public String asString() {
        return "Int";
    }

    @Override
    public boolean isCompatible(Type target) {
        return equals(target) || target instanceof AnyType;
    }
}
