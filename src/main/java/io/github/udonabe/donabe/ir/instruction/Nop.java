package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.compile.code.instruction.OpCode;
import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;

public record Nop(IRLocation location) implements Instruction {

    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitNop(this);
    }

    @Override
    public OpCode opcode() {
        return OpCode.NOP;
    }

    @Override
    public int size() {
        return 0x01;
    }
}
