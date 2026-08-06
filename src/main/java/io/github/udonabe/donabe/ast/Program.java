package io.github.udonabe.donabe.ast;

import io.github.udonabe.donabe.ast.statement.Statement;

import java.util.List;

public record Program(List<Statement> statements, SourceFileLocation location) implements ASTNode {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitProgram(this);
    }
}
