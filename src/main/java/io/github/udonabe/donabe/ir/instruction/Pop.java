package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.compile.code.OpCode;
import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;

public record Pop(IRLocation location) implements Instruction {
    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitPop(this);
    }
    
        @Override
    public OpCode opcode() {
        return OpCode.POP; 
    }
}
