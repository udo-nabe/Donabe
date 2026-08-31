package io.github.udonabe.donabe.semantic.type;

import io.github.udonabe.donabe.semantic.type.builtin.BuiltinType;
import io.github.udonabe.donabe.semantic.type.function.FunctionType;

import java.util.Map;

public sealed interface Type
        permits BuiltinType, FunctionType {
    String asString();
    Type parent();
    Map<String, MemberInfo> members();
    default MemberInfo findMember(String name) {
        if (members().containsKey(name)) {
            return members().get(name);
        }
        if (parent() == null) {
            return null;
        }
        return parent().findMember(name);
    }
    default boolean isSupertypeOf(Type target) {
        return target.isSubtypeOf(this);
    }
    boolean isSubtypeOf(Type target);
}
