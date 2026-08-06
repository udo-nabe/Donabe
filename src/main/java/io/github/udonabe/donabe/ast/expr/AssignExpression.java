package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record AssignExpression(Expression target, Expression value, SourceFileLocation location) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitAssignExpression(this);
    }

    @Override
    public String display() {
        return target().display() + " = " + value.display();
    }
}
