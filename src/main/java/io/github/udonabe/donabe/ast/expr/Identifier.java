package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

import java.util.Objects;

public record Identifier(String name, SourceFileLocation location) implements Expression {

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitIdentifier(this);
    }

    @Override
    public String display() {
        return name;
    }
}
