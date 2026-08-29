package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.ir.instruction.Instruction;
import io.github.udonabe.donabe.runtime.InterpreterException;

import java.util.List;
import java.util.Set;

public record FunctionValue(String name,
                            List<Integer> paramSlots,
                            Set<Integer> locals,
                            List<Instruction> instructions) implements RuntimeValue<String> {
    @Override
    public String value() {
        throw new UnsupportedOperationException("Undefined identifier.");
    }

    @Override
    public String typeName() {
        throw new UnsupportedOperationException("Undefined identifier.");
    }

    @Override
    public String display() {
        throw new UnsupportedOperationException("Undefined identifier.");
    }

    @Override
    public RuntimeValue<?> getMember(String name) {
        throw new UnsupportedOperationException("Undefined identifier.");
    }
}
