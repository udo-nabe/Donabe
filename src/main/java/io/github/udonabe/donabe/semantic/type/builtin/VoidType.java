package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.Type;

public record VoidType() implements BuiltinType {
    @Override
    public String asString() {
        return "Void";
    }

    @Override
    public boolean isSubtypeOf(Type target) {
        return target instanceof VoidType;
    }
}
