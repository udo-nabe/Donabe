package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.Type;

public record ListType(Type elementType) implements BuiltinType {
    @Override
    public String asString() {
        return "List<" + elementType.asString() + ">";
    }

    @Override
    public boolean isSubtypeOf(Type target) {
        if (target instanceof AnyType) {
            return true;
        }
        if (!(target instanceof ListType listType)) {
            return false;
        }
        return this.elementType.isSubtypeOf(listType.elementType);
    }
}
