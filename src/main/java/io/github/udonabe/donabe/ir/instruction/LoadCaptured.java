package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.compile.code.instruction.OpCode;
import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;

public record LoadCaptured(int depth, int identifierSlot, IRLocation location) implements Instruction {

    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitLoadCaptured(this);
    }

    @Override
    public OpCode opcode() {
        return OpCode.LOAD_CAPTURED;
    }

    @Override
    public int size() {
        return 0x05;
    }
}
