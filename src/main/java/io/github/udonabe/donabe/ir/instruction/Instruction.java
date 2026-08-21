package io.github.udonabe.donabe.ir.instruction;

public sealed interface Instruction permits Add, Call, Div, Equal, Greater, GreaterEqual, Jmp, JmpFalse, JmpTrue, LabelNop, Less, LessEqual, Load, Mul, Nop, Pop, Push, Store, Sub {

}
