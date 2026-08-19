package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.ast.expr.Identifier;
import io.github.udonabe.donabe.ast.statement.Statement;
import io.github.udonabe.donabe.runtime.VariableCell;
import io.github.udonabe.donabe.runtime.context.stack.StackFrame;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record FunctionValue(String name,
                            List<Identifier> formalArgs,
                            List<Statement> statements,
                            StackFrame parent,
                            Set<Integer> locals) implements RuntimeValue<String> {
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
