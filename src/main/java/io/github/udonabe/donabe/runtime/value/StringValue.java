package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.InterpreterException;

import java.util.List;
import java.util.Objects;

public record StringValue(String value) implements RuntimeValue<String> {
    public StringValue {
        Objects.requireNonNull(value);
    }
    @Override
    public String typeName() {
        return "String";
    }

    @Override
    public String display() {
        return value;
    }

    @Override
    public RuntimeValue<?> getMember(String name) {
        return switch (name) {
            case "length" -> new IntegerValue(value.length());
            case "toInt" -> new BuiltinFunctionValue(
                    List.of(),
                    args -> {
                        if (!value.chars().allMatch(ch -> Character.isDigit((char) ch))) {
                            throw new InterpreterException("Could not convert 'String' to 'Int'.");
                        }
                        return new IntegerValue(Integer.parseInt(value));
                    }
            );
            default -> throw new InterpreterException("The type 'String' does not have member '%s'".formatted(name));
        };
    }
}
