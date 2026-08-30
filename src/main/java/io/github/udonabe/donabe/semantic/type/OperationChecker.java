package io.github.udonabe.donabe.semantic.type;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.error.ErrorUtil;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.BinaryOperator;
import io.github.udonabe.donabe.ast.expr.CompoundAssignOperator;
import io.github.udonabe.donabe.ast.expr.UnaryOperator;
import io.github.udonabe.donabe.semantic.type.builtin.BooleanType;
import io.github.udonabe.donabe.semantic.type.builtin.IntType;
import io.github.udonabe.donabe.semantic.type.builtin.StringType;
import io.github.udonabe.donabe.semantic.type.builtin.VoidType;

public final class OperationChecker {
    private final String source;

    public OperationChecker(String source) {
        this.source = source;
    }

    public Type checkBinary(Type lhsType, BinaryOperator operator, Type rhsType, SourceFileLocation location) {
        if (lhsType instanceof VoidType ||
            rhsType instanceof VoidType) {
            throw generateError(lhsType, operator, rhsType, location);
        }

        return switch (operator) {
            case PLUS -> {
                if (lhsType instanceof IntType &&
                    rhsType instanceof IntType) {
                    yield new IntType();
                }
                if (lhsType instanceof StringType &&
                    rhsType instanceof StringType) {
                    yield new StringType();
                }
                throw generateError(lhsType, operator, rhsType, location);
            }
            case MINUS, MULTIPLICATION, DIVISION -> {
                if (lhsType instanceof IntType &&
                    rhsType instanceof IntType) {
                    yield new IntType();
                }
                throw generateError(lhsType, operator, rhsType, location);
            }
            case EQUAL -> {
                yield new BooleanType();
            }

            case LESS, GREATER, LESS_EQUAL, GREATER_EQUAL -> {
                if (lhsType instanceof IntType &&
                    rhsType instanceof IntType) {
                    yield new BooleanType();
                }
                throw generateError(lhsType, operator, rhsType, location);
            }
        };
    }

    public Type checkCompoundAssign(Type lhsType, CompoundAssignOperator operator, Type rhsType, SourceFileLocation location) {
        if (lhsType instanceof VoidType ||
            rhsType instanceof VoidType) {
            throw generateError(lhsType, operator, rhsType, location);
        }

        return switch (operator) {
            case PLUS -> {
                if (lhsType instanceof IntType &&
                    rhsType instanceof IntType) {
                    yield new IntType();
                }
                if (lhsType instanceof StringType &&
                    rhsType instanceof StringType) {
                    yield new StringType();
                }
                throw generateError(lhsType, operator, rhsType, location);
            }
            case MINUS, MULTIPLICATION, DIVISION -> {
                if (lhsType instanceof IntType &&
                    rhsType instanceof IntType) {
                    yield new IntType();
                }
                throw generateError(lhsType, operator, rhsType, location);
            }
        };
    }

    public Type checkUnary(Type targetType, UnaryOperator operator, SourceFileLocation location) {
        if (targetType instanceof VoidType) {
            throw generateError(targetType, operator, location);
        }

        return switch (operator) {
            case PLUS, MINUS -> {
                if (targetType instanceof IntType) {
                    yield new IntType();
                }
                throw generateError(targetType, operator, location);
            }
            case NOT -> {
                if (targetType instanceof BooleanType) {
                    yield new BooleanType();
                }
                throw generateError(targetType, operator, location);
            }
        };
    }

    public Type checkIncrementAndDecrement(Type targetType, boolean isIncrement, SourceFileLocation location) {
        if (!(targetType instanceof IntType)) {
            String operator = isIncrement ? "++" : "--";
            throw new CompileException(ErrorUtil.makeError(location, source,
                    "The operator '%s' cannot be applied to types '%s'.",
                    operator, targetType.asString()));
        }
        return new IntType();
    }

    private CompileException generateError(Type lhsType, BinaryOperator operator, Type rhsType, SourceFileLocation location) {
        return new CompileException(ErrorUtil.makeError(location, source,
                "The operator '%s' cannot be applied to types '%s' and '%s'.",
                operator.display(), lhsType.asString(), rhsType.asString()));
    }
    private CompileException generateError(Type lhsType, UnaryOperator operator, SourceFileLocation location) {
        return new CompileException(ErrorUtil.makeError(location, source,
                "The operator '%s' cannot be applied to types '%s'.",
                operator.display(), lhsType.asString()));
    }
    private CompileException generateError(Type lhsType, CompoundAssignOperator operator, Type rhsType, SourceFileLocation location) {
        return new CompileException(ErrorUtil.makeError(location, source,
                "The operator '%s' cannot be applied to types '%s' and '%s'.",
                operator.display(), lhsType.asString(), rhsType.asString()));
    }
}
