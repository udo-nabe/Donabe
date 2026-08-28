package io.github.udonabe.donabe.ast.type;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Identifier;

public record NamedTypeAnnotation(Identifier name) implements TypeAnnotation {
    @Override
    public SourceFileLocation location() {
        return name.location();
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitNamedTypeAnnotation(this);
    }

    @Override
    public String typeString() {
        return name.name();
    }
}
