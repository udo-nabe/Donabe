package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record Decrement(Identifier target, boolean prefix, SourceFileLocation location) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitDecrement(this);
    }

    @Override
    public String display() {
        return prefix ?
                "--" + target.display() :
                target().display() + "--";
    }
}
