package io.github.udonabe.donabe.runtime.context;

import io.github.udonabe.donabe.ir.instruction.Instruction;
import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.VariableCell;
import io.github.udonabe.donabe.runtime.context.stack.StackFrame;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.*;

public class InterpretContext {
    private StackFrame currentFrame;
    private final Deque<RuntimeValue<?>> stack;

    public InterpretContext(List<Instruction> instructions, Map<Integer, VariableCell> rootStackFrame) {
        currentFrame = new StackFrame(null, "<root>", instructions, rootStackFrame);
        stack = new ArrayDeque<>();
    }

    public void pushStackFrame(StackFrame newStackFrame) {
        currentFrame = newStackFrame;
    }

    public StackFrame popStackFrame() {
        if (!currentFrame.hasCaller()) {
            throw new IllegalStateException("Cannot call popStackFrame when callStack is 1 or less.");
        }
        currentFrame = currentFrame.caller();
        return currentFrame;
    }

    public StackFrame peekStackFrame() {
        return currentFrame;
    }

    public Instruction currentInstruction() {
        return currentFrame.instructions().get(currentFrame.registers().pc());
    }

    public VariableCell getVar(int slot, boolean isLocal) {
        return currentFrame.getVariable(slot, isLocal);
    }

    public void setVarValue(int slot, RuntimeValue<?> value, boolean isLocal) {
        getVar(slot, isLocal).setValue(value);
    }

    public void pushStack(RuntimeValue<?> value) {
        stack.push(value);
    }

    public RuntimeValue<?> popStack() {
        if (stack.isEmpty()) {
            throw new InterpreterException("Stack is empty. pc=" + currentFrame.registers().pc());
        }
        return stack.pop();
    }

    public List<RuntimeValue<?>> snapshotStack() {
        return List.copyOf(stack);
    }

    public int pc() {
        return currentFrame.registers().pc();
    }

    public void incrementPC() {
        currentFrame.registers().incrementPC();
    }

    public void setPC(int pc) {
        currentFrame.registers().setPC(pc);
    }

    public boolean isFinished() {
        return currentFrame.registers().pc() >= currentFrame.instructions().size();
    }
}
