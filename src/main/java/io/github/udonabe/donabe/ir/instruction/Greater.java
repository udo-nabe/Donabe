package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.compile.code.instruction.OpCode;
import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;

public record Greater(IRLocation location) implements Instruction {

    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitGreater(this);
    }

    @Override
    public OpCode opcode() {
        return OpCode.GREATER;
    }

    @Override
    public int size() {
        return 0x01;
    }
}
