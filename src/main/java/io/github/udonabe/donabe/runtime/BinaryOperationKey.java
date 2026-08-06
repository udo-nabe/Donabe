package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.ast.expr.BinaryOperator;

public record BinaryOperationKey(BinaryOperator operator,
                                 Class<?> left,
                                 Class<?> right) {
}
