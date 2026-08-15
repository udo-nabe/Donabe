package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.ast.expr.Identifier;
import io.github.udonabe.donabe.ast.statement.Statement;

import java.util.List;

public record FunctionValue(List<Identifier> formalArgs, List<Statement> statements) implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<function(" + formalArgs + "->" + "?)>";
    }
    @Override
    public String typeName() {
        return "function";
    }

    @Override
    public String display() {
        return value();
    }
}
