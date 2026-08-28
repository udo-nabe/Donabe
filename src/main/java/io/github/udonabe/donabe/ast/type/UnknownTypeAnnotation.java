package io.github.udonabe.donabe.ast.type;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record UnknownTypeAnnotation(SourceFileLocation location) implements TypeAnnotation {
    @Override
    public String typeString() {
        return "";
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        throw new AssertionError();
    }
}
