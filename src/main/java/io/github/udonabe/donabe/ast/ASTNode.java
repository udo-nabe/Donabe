package io.github.udonabe.donabe.ast;


public interface ASTNode {
    SourceFileLocation location();
    public <R> R accept(ASTVisitor<R> visitor);
}
