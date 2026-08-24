package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.ast.expr.BinaryOperator;
import io.github.udonabe.donabe.ast.expr.UnaryOperator;
import io.github.udonabe.donabe.runtime.value.*;

public final class Operations {
    public Operations() {
    }

    public String errorBinary(BinaryOperator operator, RuntimeValue<?> left, RuntimeValue<?> right) {
        return String.format("Operator '%s' cannot be applied to types '%s' and '%s'.", operator, left.typeName(), right.typeName());
    }

    public RuntimeValue<?> applyBinary(BinaryOperator operator,
                                       RuntimeValue<?> left,
                                       RuntimeValue<?> right) {
        return switch (operator) {
            case PLUS -> {
                //どちらかがStringの場合、Stringを返す。
                if (left instanceof StringValue str) {
                    yield new StringValue(str.display() + right.display());
                } else if (right instanceof StringValue str) {
                    yield new StringValue(left.display() + str.display());
                }

                //そうでない場合、どちらかがintでないならエラー
                if (left instanceof IntegerValue(Integer l) &&
                    right instanceof IntegerValue(Integer r)) {
                    yield new IntegerValue(l + r);
                }
                throw new InterpreterException(errorBinary(operator, left, right));
            }
            case MINUS -> {
                //どちらかがintでない場合、エラー
                if (left instanceof IntegerValue(Integer l) &&
                    right instanceof IntegerValue(Integer r)) {
                    yield new IntegerValue(l - r);
                }
                throw new InterpreterException(errorBinary(operator, left, right));
            }
            case MULTIPLICATION -> {
                //どちらかがintでない場合、エラー
                if (left instanceof IntegerValue(Integer l) &&
                    right instanceof IntegerValue(Integer r)) {
                    yield new IntegerValue(l * r);
                }
                throw new InterpreterException(errorBinary(operator, left, right));
            }
            case DIVISION -> {
                //どちらかがintでない場合、エラー
                if (left instanceof IntegerValue(Integer l) &&
                    right instanceof IntegerValue(Integer r)) {
                    yield new IntegerValue(l / r);
                }
                throw new InterpreterException(errorBinary(operator, left, right));
            }
            case EQUAL -> new BooleanValue(left.equals(right));
            case LESS -> {
                //どちらかがintでない場合、エラー
                if (left instanceof IntegerValue(Integer l) &&
                    right instanceof IntegerValue(Integer r)) {
                    yield new BooleanValue(l < r);
                }
                throw new InterpreterException(errorBinary(operator, left, right));
            }
            case GREATER -> {
                //どちらかがintでない場合、エラー
                if (left instanceof IntegerValue(Integer l) &&
                    right instanceof IntegerValue(Integer r)) {
                    yield new BooleanValue(l > r);
                }
                throw new InterpreterException(errorBinary(operator, left, right));
            }
            case LESS_EQUAL -> {
                //どちらかがintでない場合、エラー
                if (left instanceof IntegerValue(Integer l) &&
                    right instanceof IntegerValue(Integer r)) {
                    yield new BooleanValue(l <= r);
                }
                throw new InterpreterException(errorBinary(operator, left, right));
            }
            case GREATER_EQUAL -> {
                //どちらかがintでない場合、エラー
                if (left instanceof IntegerValue(Integer l) &&
                    right instanceof IntegerValue(Integer r)) {
                    yield new BooleanValue(l >= r);
                }
                throw new InterpreterException(errorBinary(operator, left, right));
            }
        };
    }

    private String errorUnary(UnaryOperator operator, RuntimeValue<?> target) {
        return String.format("Operator '%s' cannot be applied to type '%s'.", operator, target.typeName());
    }

    public RuntimeValue<?> applyUnary(UnaryOperator operator, RuntimeValue<?> target) {
        return switch (operator) {
            case MINUS -> {
                if (target instanceof IntegerValue(Integer value)) {
                    yield new IntegerValue(-value);
                }
                throw new InterpreterException(errorUnary(operator, target));
            }
            case PLUS -> {
                if (target instanceof IntegerValue(Integer value)) {
                    yield new IntegerValue(+value);
                }
                throw new InterpreterException(errorUnary(operator, target));
            }
            case NOT -> {
                if (target instanceof BooleanValue(Boolean value)) {
                    yield new BooleanValue(!value);
                }
                throw new InterpreterException(errorUnary(operator, target));
            }
        };
    }
}
