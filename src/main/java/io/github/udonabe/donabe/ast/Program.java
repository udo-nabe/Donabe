package io.github.udonabe.donabe.ast;

import java.util.List;

import io.github.udonabe.donabe.ast.statement.Definition;

public record Program(List<Definition> definitions, SourceFileLocation location) implements ASTNode {
	@Override
	public <R> R accept(ASTVisitor<R> visitor) {
		return visitor.visitProgram(this);
	}
}
