package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;

public record Greater(IRLocation location) implements Instruction {
    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitGreater(this);
    }
}
