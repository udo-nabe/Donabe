package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;

public record LoadCaptured(int identifierSlot, IRLocation location) implements Instruction {
    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitLoadCaptured(this);
    }
}
