package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.MemberInfo;
import io.github.udonabe.donabe.semantic.type.Type;

import java.util.Map;

public record Int64Type() implements BuiltinType {
    @Override
    public String asString() {
        return "Int64";
    }

    @Override
    public Type parent() {
        return new AnyType();
    }

    @Override
    public Map<String, MemberInfo> members() {
        return Map.of();
    }

    @Override
    public boolean isSubtypeOf(Type target) {
        return target instanceof AnyType ||
               target instanceof Int64Type;
    }
}
