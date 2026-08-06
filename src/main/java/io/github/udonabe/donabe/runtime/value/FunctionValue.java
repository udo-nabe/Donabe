package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.ast.statement.Statement;
import io.github.udonabe.donabe.runtime.VariableCell;

import java.util.List;
import java.util.Map;

public record FunctionValue(List<String> argNames, List<Statement> statements, Map<String, VariableCell> captures) implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<function(" + argNames + "->" + "?)>";
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
