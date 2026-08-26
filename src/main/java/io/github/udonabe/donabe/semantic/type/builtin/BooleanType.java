package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.Type;

public record BooleanType() implements BuiltinType {
    @Override
    public String asString() {
        return "Bool";
    }

    @Override
    public boolean isCompatible(Type target) {
        return equals(target) || target instanceof AnyType;
    }
}
