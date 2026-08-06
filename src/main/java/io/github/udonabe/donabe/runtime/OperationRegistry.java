package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.ast.expr.BinaryOperator;
import io.github.udonabe.donabe.ast.expr.UnaryOperator;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class OperationRegistry {
    private final Map<BinaryOperationKey, BiFunction<RuntimeValue<?>, RuntimeValue<?>, RuntimeValue<?>>> binaryMap;
    private final Map<UnaryOperationKey, Function<RuntimeValue<?>, RuntimeValue<?>>> unaryMap;

    public OperationRegistry() {
        binaryMap = new HashMap<>();
        unaryMap = new HashMap<>();
    }

    public <L extends RuntimeValue<?>, R extends RuntimeValue<?>> void registerBinary(
            BinaryOperator operator,
            Class<L> left,
            Class<R> right,
            BiFunction<L, R, RuntimeValue<?>> function
    ) {
        binaryMap.put(
                new BinaryOperationKey(operator, left, right),
                (l, r) -> function.apply(left.cast(l), right.cast(r))
                );
    }
    public <E extends RuntimeValue<?>> void registerUnary(
            UnaryOperator operator,
            Class<E> target,
            Function<E, RuntimeValue<?>> function
    ) {
        unaryMap.put(
            new UnaryOperationKey(operator, target),
                (e) -> function.apply(target.cast(e))
        );
    }

    public RuntimeValue<?> applyBinary(BinaryOperator operator,
                                       RuntimeValue<?> left,
                                       RuntimeValue<?> right) {
        BiFunction<RuntimeValue<?>, RuntimeValue<?>, RuntimeValue<?>> function = binaryMap.get(new BinaryOperationKey(operator, left.getClass(), right.getClass()));
        if (function == null) {
            throw new InterpreterException(String.format("Operator '%s' cannot be applied to types '%s' and '%s'.", operator, left.typeName(), right.typeName()));
        }
        return function.apply(left, right);
    }

    public RuntimeValue<?> applyUnary(UnaryOperator operator, RuntimeValue<?> target) {
        Function<RuntimeValue<?>, RuntimeValue<?>> function = unaryMap.get(new UnaryOperationKey(operator, target.getClass()));
        if (function == null) {
            throw new InterpreterException(String.format("Operator '%s' cannot be applied to type '%s'.", operator, target.getClass()));
        }
        return function.apply(target);
    }
}
