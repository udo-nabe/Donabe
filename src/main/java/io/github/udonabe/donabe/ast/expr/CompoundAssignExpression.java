package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record CompoundAssignExpression(Expression target, CompoundAssignOperator operator, Expression value, SourceFileLocation location) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitCompoundAssignExpression(this);
    }

    @Override
    public String display() {
        return target().display() + " " + operator.display() + " " + value.display();
    }
}
