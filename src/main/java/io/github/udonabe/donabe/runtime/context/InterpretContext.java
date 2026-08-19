package io.github.udonabe.donabe.runtime.context;

import io.github.udonabe.donabe.runtime.VariableCell;
import io.github.udonabe.donabe.runtime.context.stack.StackFrame;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.HashMap;
import java.util.Map;

public class InterpretContext {
    private StackFrame currentFrame;

    public InterpretContext(Map<Integer, VariableCell> rootStackFrame) {
        currentFrame = new StackFrame(null, "<root>", rootStackFrame, rootStackFrame.keySet());
    }

    public void pushStackFrame(StackFrame newStackFrame) {
        currentFrame = newStackFrame;
    }

    public StackFrame popStackFrame() {
        if (!currentFrame.hasParent()) {
            throw new IllegalStateException("Cannot call popStackFrame when callStack is 1 or less.");
        }
        currentFrame = currentFrame.parent();
        return currentFrame;
    }

    public StackFrame peekStackFrame() {
        return currentFrame;
    }

    public VariableCell getVar(int slot) {
        return currentFrame.getVariable(slot);
    }

    public void setVarValue(int slot, RuntimeValue<?> value) {
        getVar(slot).setValue(value);
    }
}
