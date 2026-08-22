package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.ir.instruction.Instruction;

import java.util.List;

public record FunctionValue(String name,
                            List<Integer> paramSlots,
                            List<Instruction> instructions) implements RuntimeValue<String> {
    @Override
    public List<Instruction> instructions() {
        throw new UnsupportedOperationException("This function has not been converted to runtime type.");
    }

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
