package io.github.udonabe.donabe.ast;

import io.github.udonabe.donabe.ast.statement.Statement;

public interface ASTNode {
    SourceFileLocation location();
    public <R> R accept(ASTVisitor<R> visitor);
}
