package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.compile.code.instruction.OpCode;
import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;
import io.github.udonabe.donabe.ir.instruction.label.Label;

public record Jmp(Label label, IRLocation location) implements Instruction {

    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitJmp(this);
    }

    @Override
    public OpCode opcode() {
        return OpCode.JMP;
    }

    @Override
    public int size() {
        return 0x05;
    }
}
