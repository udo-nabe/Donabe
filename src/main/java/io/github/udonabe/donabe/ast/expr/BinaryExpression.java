package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record BinaryExpression(Expression left, BinaryOperator operator, Expression right,
                               SourceFileLocation location) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitBinaryExpression(this);
    }

    @Override
    public String display() {
        return left().display() + " " + operator.display() + " " + right.display();
    }
}
