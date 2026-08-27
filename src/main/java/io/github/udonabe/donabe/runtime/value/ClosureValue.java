package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.ir.instruction.Instruction;
import io.github.udonabe.donabe.runtime.context.stack.StackFrame;

import java.util.List;
import java.util.Set;

public record ClosureValue(String name,
                           List<Integer> paramSlots,
                           Set<Integer> locals,
                           List<Instruction> instructions,
                           StackFrame parent) implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<function(" + paramSlots + "->" + "?)>";
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
