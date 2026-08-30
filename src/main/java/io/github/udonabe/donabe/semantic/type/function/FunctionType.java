package io.github.udonabe.donabe.semantic.type.function;

import io.github.udonabe.donabe.ast.type.TypeAnnotation;
import io.github.udonabe.donabe.semantic.type.MemberInfo;
import io.github.udonabe.donabe.semantic.type.Type;
import io.github.udonabe.donabe.semantic.type.builtin.AnyType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public Type parent() {
        return new AnyType();
    }

    @Override
    public Map<String, MemberInfo> members() {
        return Map.of();
    }

    @Override
    public boolean isSubtypeOf(Type target) {
        if (target instanceof AnyType) {
            return true;
        }
        if (!(target instanceof FunctionType functionTarget)) {
            return false;
        }

        if (!this.returnType.isSubtypeOf(functionTarget.returnType)) {
            return false;
        }
        if (this.paramTypes.size() != functionTarget.paramTypes.size()) {
            return false;
        }

        return IntStream.range(0, this.paramTypes.size())
                .allMatch(i -> this.paramTypes.get(i).isSupertypeOf(functionTarget.paramTypes.get(i)));
    }

}
