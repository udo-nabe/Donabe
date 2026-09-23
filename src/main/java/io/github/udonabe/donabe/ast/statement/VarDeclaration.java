package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Expression;
import io.github.udonabe.donabe.ast.expr.Identifier;
import io.github.udonabe.donabe.ast.type.TypeAnnotation;

public record VarDeclaration(Identifier name, Expression expr, TypeAnnotation type, SourceFileLocation location) implements Statement, Definition {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitVarDeclaration(this);
    }
}
