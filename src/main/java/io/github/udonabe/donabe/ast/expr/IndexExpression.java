package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record IndexExpression(Expression target, Expression index, SourceFileLocation location) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitIndexExpression(this);
    }

    @Override
    public String display() {
        return target().display() + "[" + index.display() + "]";
    }
}
