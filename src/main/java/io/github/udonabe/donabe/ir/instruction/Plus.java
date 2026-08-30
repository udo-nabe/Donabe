package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;

public record Plus(IRLocation location) implements Instruction {
    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitPlus(this);
    }
}
