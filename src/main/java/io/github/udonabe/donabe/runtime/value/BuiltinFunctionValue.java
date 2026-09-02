package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.value.member.FunctionMemberProvider;
import io.github.udonabe.donabe.runtime.value.member.MemberProvider;

import java.util.List;
import java.util.function.Function;

public record BuiltinFunctionValue(List<String> formalArgs, Function<List<? extends RuntimeValue<?>>, RuntimeValue<?>> content) implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<builtin-function>";
    }

    @Override
    public String typeName() {
        return "Function";
    }

    @Override
    public String display() {
        return value();
    }

    @Override
    public MemberProvider<?> memberProvider() {
        return new FunctionMemberProvider();
    }
}
