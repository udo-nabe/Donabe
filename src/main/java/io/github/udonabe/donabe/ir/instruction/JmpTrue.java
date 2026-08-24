package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.ir.IRVisitor;
import io.github.udonabe.donabe.ir.instruction.label.Label;

public record JmpTrue(Label label) implements Instruction {
    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitJmpTrue(this);
    }
}
