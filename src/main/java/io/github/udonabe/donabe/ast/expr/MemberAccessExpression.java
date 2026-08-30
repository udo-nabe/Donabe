package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record MemberAccessExpression(
        Expression target,
        Identifier member,
        SourceFileLocation location) implements Expression {
    @Override
    public String display() {
        return target.display() + "." + member.display();
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitMemberAccessExpression(this);
    }
}
