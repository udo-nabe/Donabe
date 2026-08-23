package io.github.udonabe.donabe.runtime.context.stack;

import io.github.udonabe.donabe.ir.instruction.Instruction;
import io.github.udonabe.donabe.runtime.VariableCell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record StackFrame(StackFrame caller, String name, Registers registers, List<Instruction> instructions, Map<Integer, VariableCell> globals, Map<Integer, VariableCell> locals) {
    public StackFrame(StackFrame parent, String name, List<Instruction> instructions, Map<Integer, VariableCell> globalIdentifiers) {
        this(parent, name, new Registers(), instructions, globalIdentifiers, new HashMap<>());
    }

    public boolean hasCaller() {
        return caller != null;
    }
    public VariableCell getVariable(int slot, boolean isLocal) {
        if (isLocal) {
            if (locals.containsKey(slot)) {
                return locals.get(slot);
            }

            VariableCell value = globals.get(slot);

            VariableCell local = new VariableCell(value.value());
            locals.put(slot, local);
            return local;
        } else  {
            return globals.get(slot);
        }
    }
}
