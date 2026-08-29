package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.ir.IRVisitor;

public record LoadMember(String memberName) implements Instruction {
    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitLoadMember(this);
    }
}
