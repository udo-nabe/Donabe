package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.Type;

public record StringType() implements BuiltinType {
    @Override
    public String asString() {
        return "String";
    }

    @Override
    public boolean isSubtypeOf(Type target) {
        return target instanceof AnyType ||
               target instanceof StringType;
    }
}
