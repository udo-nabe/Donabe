package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record UnaryExpression(UnaryOperator operator, Expression expr, SourceFileLocation location) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitUnaryExpression(this);
    }

    @Override
    public String display() {
        return operator.display() + expr.display();
    }
}
