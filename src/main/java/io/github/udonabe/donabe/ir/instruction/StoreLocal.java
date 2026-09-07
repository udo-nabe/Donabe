package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.compile.code.instruction.OpCode;
import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRVisitor;

public record StoreLocal(int identifierSlot, IRLocation location) implements Instruction {

    @Override
    public <R> R accept(IRVisitor<R> visitor) {
        return visitor.visitStoreLocal(this);
    }

    @Override
    public OpCode opcode() {
        return OpCode.STORE_LOCAL;
    }

    @Override
    public int size() {
        return 0x03;
    }
}
