package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.MemberInfo;
import io.github.udonabe.donabe.semantic.type.Type;

import java.util.Map;

public record StringType() implements BuiltinType {
    @Override
    public String asString() {
        return "String";
    }

    @Override
    public Map<String, MemberInfo> members() {
        return Map.ofEntries(
                Map.entry("length", new MemberInfo(false, new IntType()))
        );
    }

    @Override
    public boolean isSubtypeOf(Type target) {
        return target instanceof AnyType ||
               target instanceof StringType;
    }
}
