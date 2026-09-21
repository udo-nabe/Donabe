package io.github.udonabe.donabe.ir.value;

import java.util.List;
import java.util.function.Function;

public record BuiltinFunctionValue(List<String> formalArgs, Function<List<? extends RuntimeValue<?>>, RuntimeValue<?>> content) implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<builtin-function>";
    }
}
