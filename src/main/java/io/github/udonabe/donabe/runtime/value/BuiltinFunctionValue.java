package io.github.udonabe.donabe.runtime.value;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

public record BuiltinFunctionValue(List<String> args, Function<List<RuntimeValue<?>>, RuntimeValue<?>> content) implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<builtin-function>";
    }

    @Override
    public String typeName() {
        return "function";
    }

    @Override
    public String display() {
        return value();
    }
}
