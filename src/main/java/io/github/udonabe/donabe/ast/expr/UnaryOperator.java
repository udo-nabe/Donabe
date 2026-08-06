package io.github.udonabe.donabe.ast.expr;

public enum UnaryOperator {
    MINUS("-"),
    PLUS("+"),
    NOT("!");
    private final String display;

    UnaryOperator(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
