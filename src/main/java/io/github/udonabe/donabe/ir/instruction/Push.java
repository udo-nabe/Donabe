package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.ir.IRVisitor;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

public record Push(RuntimeValue<?> value) implements Instruction {
    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitPush(this);
    }
}
