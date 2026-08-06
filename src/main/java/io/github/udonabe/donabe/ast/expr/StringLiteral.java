package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record StringLiteral(String value, SourceFileLocation location) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitStringLiteral(this);
    }

    @Override
    public String display() {
        return "\"" + value + "\"";
    }
}
