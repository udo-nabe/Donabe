package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

import java.util.List;

public record CallExpression(Expression target, List<Expression> args, SourceFileLocation location) implements Expression {
    public CallExpression {
        args = List.copyOf(args);
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitCallExpression(this);
    }

    @Override
    public String display() {
        StringBuilder arguments = new StringBuilder();
        if (!args.isEmpty()) {
            arguments.append(args.getFirst().display());
            if (args.size() > 1) {
                for (int i = 1; i < args.size(); i++) {
                    Expression expr = args.get(i);
                    arguments.append(", ");
                    arguments.append(expr.display());
                }
            }
        }
        return target.display() + "(" + arguments + ")";
    }
}
