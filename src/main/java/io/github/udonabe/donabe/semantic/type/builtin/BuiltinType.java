package io.github.udonabe.donabe.semantic.type.builtin;

import io.github.udonabe.donabe.semantic.type.Type;

public sealed interface BuiltinType extends Type
        permits AnyType, BooleanType, IntType, ListType, StringType, VoidType {
}
