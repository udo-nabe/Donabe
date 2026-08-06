package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

import java.util.List;

public record ListLiteral(List<Expression> elements, SourceFileLocation location) implements Expression {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitListLiteral(this);
    }

    @Override
    public String display() {
        StringBuilder display = new StringBuilder();
        if (!elements.isEmpty()) {
            display.append(elements.getFirst().display());
            if (elements.size() > 1) {
                for (int i = 1; i < elements.size(); i++) {
                    Expression expr = elements.get(i);
                    display.append(", ");
                    display.append(expr.display());
                }
            }
        }
        return "[" + display + "]";
    }
}
