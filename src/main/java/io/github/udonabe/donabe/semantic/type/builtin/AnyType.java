package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.MemberInfo;
import io.github.udonabe.donabe.semantic.type.Type;
import io.github.udonabe.donabe.semantic.type.function.FunctionType;

import java.util.List;
import java.util.Map;

public record AnyType() implements BuiltinType {
    @Override
    public String asString() {
        return "Any";
    }

    @Override
    public Type parent() {
        return null;
    }

    @Override
    public Map<String, MemberInfo> members() {
        return Map.of(
                "toString", new MemberInfo(false, new FunctionType(List.of(), new StringType()))
        );
    }

    @Override
    public boolean isSubtypeOf(Type target) {
        return target instanceof AnyType;
    }
}
