package io.github.udonabe.donabe.ast;

import io.github.udonabe.donabe.ast.expr.Identifier;
import io.github.udonabe.donabe.ast.type.TypeAnnotation;

public record Parameter(Identifier name, TypeAnnotation type) implements ASTNode {
    @Override
    public SourceFileLocation location() {
        return name.location();
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitParameter(this);
    }
}
