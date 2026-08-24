package io.github.udonabe.donabe.runtime.context.stack;

import io.github.udonabe.donabe.ir.IRViewer;
import io.github.udonabe.donabe.ir.instruction.Instruction;
import io.github.udonabe.donabe.ir.instruction.LoadLocal;
import io.github.udonabe.donabe.ir.instruction.StoreLocal;
import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.VariableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StackFrame {
    private static final Logger log = LoggerFactory.getLogger(StackFrame.class);

    private final StackFrame caller;
    private final StackFrame parent;
    private final String name;
    private final Registers registers;
    private final List<Instruction> instructions;
    private final Map<Integer, VariableCell> locals;
    private final Map<Integer, VariableCell> capturedCache;


    public StackFrame(StackFrame caller, StackFrame parent, String name, Registers registers, List<Instruction> instructions, Map<Integer, VariableCell> global) {
        this.caller = caller;
        this.parent = parent;
        this.name = name;
        this.registers = registers;
        this.instructions = instructions;

        Map<Integer, VariableCell> locals = new HashMap<>();
        for (Instruction instruction : instructions) {
            switch (instruction) {
                case LoadLocal(int identifierSlot) -> locals.put(identifierSlot, new VariableCell(global.get(identifierSlot).value()));
                case StoreLocal(int identifierSlot) -> locals.put(identifierSlot, new VariableCell(global.get(identifierSlot).value()));
                default -> {}
            }
        }
        this.locals = locals;
        log.trace("Local slots: {} frame={}", locals.keySet(), name);
        capturedCache = new HashMap<>();
    }

    public StackFrame(StackFrame caller, StackFrame parent, String name, List<Instruction> instructions, Map<Integer, VariableCell> globals) {
        this(caller, parent, name, new Registers(), instructions, globals);
    }

    public boolean hasCaller() {
        return caller != null;
    }

    public VariableCell getLocal(int slot) {
        if (!locals.containsKey(slot)) {
            throw new InterpreterException("Local variable not found. frame=" + name + " slot=" + slot);
        }
        log.trace("Found local variable. frame={} slot={}", name, slot);
        return locals.get(slot);
    }

    public VariableCell getCaptured(int slot) {
        if (parent == null) {
            throw new InterpreterException("Could not find captured variable: parent is null. frame=" + name);
        }
        if (capturedCache.containsKey(slot)) {
            log.trace("Found captured variable in cache. frame={} slot={}", name, slot);
            return capturedCache.get(slot);
        }

        log.trace("Begin to find local variable... frame={} slot={}", name, slot);

        VariableCell capturedCell = parent.getCapturedImpl(slot);

        log.trace("Cache captured variable. frame={} slot={}", name, slot);
        capturedCache.put(slot, capturedCell);

        return capturedCell;
    }

    private VariableCell getCapturedImpl(int slot) {
        if (locals.containsKey(slot)) {
            log.trace("Found captured local variable. frame={} slot={}", name, slot);
            return locals.get(slot);
        } else {
            if (parent == null) {
                throw new InterpreterException("Could not find captured variable: parent is null. frame=" + name);
            }
            log.trace("Delegate to parent to find local variable parent. frame={} slot={}", name, slot);
            return parent.getCapturedImpl(slot);
        }
    }

    public StackFrame caller() {
        return caller;
    }

    public StackFrame parent() {
        return parent;
    }

    public String name() {
        return name;
    }

    public Registers registers() {
        return registers;
    }

    public List<Instruction> instructions() {
        return instructions;
    }

    public Map<Integer, VariableCell> locals() {
        return locals;
    }
}
