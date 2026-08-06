package io.github.udonabe.donabe.ast.expr;

public enum BinaryOperator {
    PLUS("+"),
    MINUS("-"),
    MULTIPLICATION("*"),
    DIVISION("/"),
    EQUAL("=="),
    LESS(">"),
    GREATER("<"),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">=");
    private final String display;

    BinaryOperator(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
