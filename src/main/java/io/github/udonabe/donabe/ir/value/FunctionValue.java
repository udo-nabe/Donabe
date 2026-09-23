package io.github.udonabe.donabe.ir.value;

import io.github.udonabe.donabe.ir.instruction.Instruction;

import java.util.List;

public record FunctionValue(String name,
                            List<Integer> paramSlots,
                            int localCount,
                            List<Instruction> instructions) implements RuntimeValue<String> {
    @Override
    public String value() {
        throw new UnsupportedOperationException("Undefined identifier.");
    }
}
