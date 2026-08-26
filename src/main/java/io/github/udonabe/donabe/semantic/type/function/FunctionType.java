package io.github.udonabe.donabe.semantic.type.function;

import io.github.udonabe.donabe.ast.type.TypeAnnotation;
import io.github.udonabe.donabe.semantic.type.Type;
import io.github.udonabe.donabe.semantic.type.builtin.AnyType;

import java.util.List;
import java.util.stream.Collectors;

public record FunctionType(
        List<Type> paramTypes,
        Type returnType) implements Type {
    @Override
    public String asString() {
        String paramString = paramTypes.stream()
                .map(Type::asString)
                .collect(Collectors.joining(", ", "(", ")"));
        return paramString + " -> " + returnType.asString();
    }

    @Override
    public boolean isCompatible(Type target) {
        return equals(target) || target instanceof AnyType;
    }
}
