package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.compile.code.OpCode;
import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;
import io.github.udonabe.donabe.ir.instruction.label.Label;

public record JmpFalse(Label label, IRLocation location) implements Instruction {
    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitJmpFalse(this);
    }
    
        @Override
    public OpCode opcode() {
        return OpCode.JMP_FALSE; 
    }
}
