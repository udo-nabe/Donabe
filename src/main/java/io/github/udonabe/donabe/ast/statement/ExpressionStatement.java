package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Expression;

public record ExpressionStatement(Expression expression, SourceFileLocation location) implements Statement {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitExpressionStatement(this);
    }
}
