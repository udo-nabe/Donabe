package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.MemberInfo;
import io.github.udonabe.donabe.semantic.type.Type;

import java.util.Map;

public record IntType() implements BuiltinType {
    @Override
    public String asString() {
        return "Int";
    }

    @Override
    public Map<String, MemberInfo> members() {
        return Map.of();
    }

    @Override
    public boolean isSubtypeOf(Type target) {
        return target instanceof AnyType ||
               target instanceof IntType;
    }
}
