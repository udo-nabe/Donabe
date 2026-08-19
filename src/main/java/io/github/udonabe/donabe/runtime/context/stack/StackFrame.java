package io.github.udonabe.donabe.runtime.context.stack;

import io.github.udonabe.donabe.runtime.VariableCell;

import java.util.Map;
import java.util.Set;

public record StackFrame(StackFrame parent, String name, Map<Integer, VariableCell> locals, Set<Integer> localSlots) {
    public boolean hasParent() {
        return parent != null;
    }
    public VariableCell getVariable(int slot) {
        if (locals.containsKey(slot)) {
            return locals.get(slot);
        }

        VariableCell value = parent.getVariable(slot);

        if (localSlots.contains(slot)) {
            VariableCell local = new VariableCell(value.mutable(), value.value());
            locals.put(slot, local);
            return local;
        } else {
            return value;
        }
    }
}
