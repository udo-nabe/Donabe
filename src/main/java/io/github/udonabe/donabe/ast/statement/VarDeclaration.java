package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Expression;
import io.github.udonabe.donabe.ast.expr.Identifier;

public record VarDeclaration(Identifier name, Expression expr, SourceFileLocation location) implements Statement {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitVarDeclaration(this);
    }
}
