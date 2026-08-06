package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

import java.util.List;

public record BlockStatement(List<Statement> statements, SourceFileLocation location) implements Statement {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitBlockStatement(this);
    }
}
