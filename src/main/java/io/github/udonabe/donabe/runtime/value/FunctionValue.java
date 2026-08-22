package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.ir.instruction.Instruction;

import java.util.List;

public record FunctionValue(String name,
                            List<Integer> paramSlots,
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
}
