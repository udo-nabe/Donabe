package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public record CallExpression(SourceFileLocation location, Expression target, Expression... args) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitCallExpression(this);
    }

    @Override
    public String display() {
        StringBuilder arguments = new StringBuilder();
        if (args.length != 0) {
            arguments.append(args[0].display());
            if (args.length > 1) {
                for (int i = 1; i < args.length; i++) {
                    Expression expr = args[i];
                    arguments.append(", ");
                    arguments.append(expr.display());
                }
            }
        }
        return target.display() + "(" + arguments + ")";
    }
}
