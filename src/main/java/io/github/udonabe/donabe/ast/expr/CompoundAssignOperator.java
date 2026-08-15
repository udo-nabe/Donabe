package io.github.udonabe.donabe.ast.expr;

public enum CompoundAssignOperator {
    PLUS("+="),
    MINUS("-="),
    MULTIPLICATION("*="),
    DIVISION("/=");
    private final String display;

    CompoundAssignOperator(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
