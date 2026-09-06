package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.compile.code.instruction.OpCode;
import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

public record Push(RuntimeValue<?> value, IRLocation location) implements Instruction {

    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitPush(this);
    }

    @Override
    public OpCode opcode() {
        return OpCode.PUSH;
    }

    @Override
    public int size() {
        return 0x05;
    }
}
