package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.statement.BlockStatement;

import java.util.List;

public record FunctionLiteral(List<Identifier> args, BlockStatement block, SourceFileLocation location) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitFunctionLiteral(this);
    }

    @Override
    public String display() {
        return "<function(" + args.stream().map(Identifier::name).toList() + "->?)>";
    }
}
