package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.ast.expr.UnaryOperator;

public record UnaryOperationKey(UnaryOperator operator,
                                Class<?> target) {
}
