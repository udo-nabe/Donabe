package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment parent;
    private final Map<String, VariableCell> vars;

    public Environment(Environment parent) {
        this.vars = new HashMap<>();
        this.parent = parent;
    }

    public void declare(String symbol, VariableCell value) {
        if (!vars.containsKey(symbol)) {
            vars.put(symbol, value);
            return;
        }
        throw new InterpreterException("Variable '" + symbol + "' is already declared.");
    }
    public void initialize(String symbol, RuntimeValue<?> value) {
        if (!vars.containsKey(symbol)) {
            throw new InterpreterException("Variable '" + symbol + "' is not declared.");
        }
        if (!vars.get(symbol).temporary()) {
            throw new InterpreterException("Variable '" + symbol + "' is not temporary.");
        }
        if (vars.get(symbol).value().getClass() != value.getClass()) {
            throw new InterpreterException("Variable '" + symbol + "' is different type.");
        }
        vars.get(symbol).setValue(value);
    }
    public void declareForce(String symbol, VariableCell value) {
        vars.put(symbol, value);
    }

    public RuntimeValue<?> assign(String symbol, RuntimeValue<?> value) {
        VariableCell val = getVarRaw(symbol);
        if (!val.mutable()) {
            throw new InterpreterException("'let " + symbol + "' is immutable.");
        }
        assignImpl(symbol, value);
        return value;
    }

    private void assignImpl(String symbol, RuntimeValue<?> value) {
        if (vars.containsKey(symbol)) {
            vars.get(symbol).setValue(value);
        } else {
            parent.assignImpl(symbol, value);
        }
    }

    public VariableCell getVarRaw(String symbol) {
        if (!vars.containsKey(symbol)) {
            if (parent == null) {
                throw new InterpreterException("Unknown target: '" + symbol + "'.");
            } else {
                return parent.getVarRaw(symbol);
            }
        }
        return vars.get(symbol);
    }

    public RuntimeValue<?> getVar(String symbol) {
        return getVarRaw(symbol).value();
    }

    public Map<String, VariableCell> getVarsRecursive() {
        if (parent == null) {
            return Map.copyOf(vars);
        }
        Map<String, VariableCell> result = new HashMap<>(vars);
        Map<String, VariableCell> parentVars = parent.getVarsRecursive();
        for (String key : parentVars.keySet()) {
            result.putIfAbsent(key, parentVars.get(key));
        }
        return Map.copyOf(result);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Environment{");
        sb.append("vars=").append(vars);
        sb.append('}');
        return sb.toString();
    }

    public Environment parent() {
        return parent;
    }
}
