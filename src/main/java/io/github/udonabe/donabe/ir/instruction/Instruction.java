package io.github.udonabe.donabe.ir.instruction;

public sealed interface Instruction permits Add, Call, Div, Equal, Greater, GreaterEqual, Jmp, JmpFalse, JmpTrue, LabelNop, Less, LessEqual, Load, LoadLocal, Mul, Nop, Pop, Push, Return, Store, StoreLocal, Sub, VoidReturn {

}
