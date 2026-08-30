package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.MemberInfo;
import io.github.udonabe.donabe.semantic.type.Type;

import java.util.Map;

public record ListType(Type elementType) implements BuiltinType {
    @Override
    public String asString() {
        return "List<" + elementType.asString() + ">";
    }

    @Override
    public Type parent() {
        return new AnyType();
    }

    @Override
    public Map<String, MemberInfo> members() {
        return Map.ofEntries(
                Map.entry("length", new MemberInfo(false, new IntType()))
        );
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
