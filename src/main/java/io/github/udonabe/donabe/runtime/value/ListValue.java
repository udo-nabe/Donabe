package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.InterpreterException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ListValue(List<RuntimeValue<?>> value) implements RuntimeValue<List<RuntimeValue<?>>> {
    @Override
    public String typeName() {
        return "List";
    }

    @Override
    public String display() {
        List<RuntimeValue<?>> forShow = new ArrayList<>(value);
        if (forShow.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        sb.append(forShow.removeFirst().value());
        for (RuntimeValue<?> r : forShow) {
            sb.append(", ").append(r.display());
        }
        return sb.append("]").toString();
    }

    @Override
    public RuntimeValue<?> getMember(String name) {
        return switch (name) {
            case "length" -> new IntegerValue(value.size());
            default -> throw new InterpreterException("The type 'List' does not have member '%s'".formatted(name));
        };
    }
}
