package io.github.udonabe.donabe.runtime.context;

import io.github.udonabe.donabe.ir.instruction.Instruction;
import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.VariableCell;
import io.github.udonabe.donabe.runtime.context.stack.StackFrame;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.*;

public class InterpretContext {
    private final int MAX_CALL_STACK_DEPTH = 1000;
    private int callDepth;
    private StackFrame currentFrame;
    private final Deque<RuntimeValue<?>> stack;

    public InterpretContext(List<Instruction> instructions, Set<Integer> resolution) {
        currentFrame = new StackFrame(null, null, "<root>", instructions, resolution);
        stack = new ArrayDeque<>();
        callDepth = 1;
    }

    public void pushStackFrame(StackFrame newStackFrame) {
        if (callDepth++ >= MAX_CALL_STACK_DEPTH) {
            throw new InterpreterException("Stack overflow.", currentFrame);
        }
        currentFrame = newStackFrame;
    }

    public StackFrame popStackFrame() {
        if (!currentFrame.hasCaller()) {
            throw new InterpreterException("Cannot call popStackFrame when callStack is 1 or less.", currentFrame);
        }
        currentFrame = currentFrame.caller();
        callDepth--;
        return currentFrame;
    }

    public StackFrame peekStackFrame() {
        return currentFrame;
    }

    public Instruction currentInstruction() {
        return currentFrame.instructions().get(currentFrame.registers().pc());
    }

    public VariableCell getLocal(int slot) {
        return currentFrame.getLocal(slot);
    }

    public VariableCell getCaptured(int slot) {
        return currentFrame.getCaptured(slot);
    }

    public void setLocalVarValue(int slot, RuntimeValue<?> value) {
        getLocal(slot).setValue(value);
    }

    public void setCapturedVarValue(int slot, RuntimeValue<?> value) {
        getCaptured(slot).setValue(value);
    }

    public void pushStack(RuntimeValue<?> value) {
        stack.push(value);
    }

    public RuntimeValue<?> popStack() {
        if (stack.isEmpty()) {
            throw new InterpreterException("Stack is empty.", currentFrame);
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
