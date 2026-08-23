package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.ast.expr.BinaryOperator;
import io.github.udonabe.donabe.ast.expr.UnaryOperator;
import io.github.udonabe.donabe.runtime.value.*;

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

    public static OperationRegistry generateDefault() {
        OperationRegistry registry = new OperationRegistry();
        // int | int
        registry.registerBinary(BinaryOperator.PLUS, IntegerValue.class, IntegerValue.class, (l, r) -> new IntegerValue(l.value() + r.value()));
        registry.registerBinary(BinaryOperator.MINUS, IntegerValue.class, IntegerValue.class, (l, r) -> new IntegerValue(l.value() - r.value()));
        registry.registerBinary(BinaryOperator.MULTIPLICATION, IntegerValue.class, IntegerValue.class, (l, r) -> new IntegerValue(l.value() * r.value()));
        registry.registerBinary(BinaryOperator.DIVISION, IntegerValue.class, IntegerValue.class, (l, r) -> new IntegerValue(l.value() / r.value()));

        // string | string
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, StringValue.class, (l, r) -> new StringValue(l.value() + r.value()));

        // string-related
        registry.registerBinary(BinaryOperator.PLUS, IntegerValue.class, StringValue.class, (l, r) -> new StringValue(l.display() + r.display()));
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, IntegerValue.class, (l, r) -> new StringValue(l.display() + r.display()));

        registry.registerBinary(BinaryOperator.PLUS, BooleanValue.class, StringValue.class, (l, r) -> new StringValue(l.display() + r.display()));
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, BooleanValue.class, (l, r) -> new StringValue(l.display() + r.display()));

        registry.registerBinary(BinaryOperator.PLUS, ClosureValue.class, StringValue.class, (l, r) -> new StringValue(l.display() + r.display()));
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, ClosureValue.class, (l, r) -> new StringValue(l.display() + r.display()));

        registry.registerBinary(BinaryOperator.PLUS, ListValue.class, StringValue.class, (l, r) -> new StringValue(l.display() + r.display()));
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, ListValue.class, (l, r) -> new StringValue(l.display() + r.display()));

        // Equal
        registry.registerBinary(BinaryOperator.EQUAL, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.equals(r)));
        registry.registerBinary(BinaryOperator.EQUAL, StringValue.class, StringValue.class, (l, r) -> new BooleanValue(l.equals(r)));
        registry.registerBinary(BinaryOperator.EQUAL, BooleanValue.class, BooleanValue.class, (l, r) -> new BooleanValue(l.equals(r)));

        // Compare
        registry.registerBinary(BinaryOperator.GREATER, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.value() > r.value()));
        registry.registerBinary(BinaryOperator.LESS, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.value() < r.value()));
        registry.registerBinary(BinaryOperator.GREATER_EQUAL, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.value() >= r.value()));
        registry.registerBinary(BinaryOperator.LESS_EQUAL, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.value() <= r.value()));

        // Unary
        registry.registerUnary(UnaryOperator.PLUS, IntegerValue.class, t -> new IntegerValue(t.value()));
        registry.registerUnary(UnaryOperator.MINUS, IntegerValue.class, t -> new IntegerValue(-t.value()));
        registry.registerUnary(UnaryOperator.NOT, BooleanValue.class, t -> new BooleanValue(!t.value()));

        return registry;
    }
}
