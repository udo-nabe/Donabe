package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Expression;

public record IfStatement(Expression condition, BlockStatement thenBlock, Statement elseBlock, SourceFileLocation location) implements Statement {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitIfStatement(this);
    }
}
