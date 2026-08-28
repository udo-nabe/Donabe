package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.ir.IRVisitor;

public sealed interface Instruction permits Add, Call, Div, Equal, Greater, GreaterEqual, Index, Jmp, JmpFalse, JmpTrue, LabelNop, Less, LessEqual, LoadCaptured, LoadLocal, MakeList, Minus, Mul, Nop, Not, Plus, Pop, Push, Return, StoreCaptured, StoreLocal, Sub, VoidReturn {
    <R> R accept(IRVisitor<R> visitor);
}
