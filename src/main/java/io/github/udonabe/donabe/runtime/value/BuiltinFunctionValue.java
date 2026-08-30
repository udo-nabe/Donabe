package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.InterpreterException;

import java.util.List;
import java.util.Map;
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
    public Map<String, RuntimeValue<?>> declaredMembers() {
        return Map.of();
    }
}
