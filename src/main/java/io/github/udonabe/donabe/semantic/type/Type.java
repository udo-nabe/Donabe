package io.github.udonabe.donabe.semantic.type;

import io.github.udonabe.donabe.semantic.type.builtin.BuiltinType;
import io.github.udonabe.donabe.semantic.type.function.FunctionType;

import java.util.Objects;

public sealed interface Type
        permits BuiltinType, FunctionType {
    String asString();
    boolean isCompatible(Type target);
}
