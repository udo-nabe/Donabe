package io.github.udonabe.donabe.ir;

import io.github.udonabe.donabe.ir.instruction.*;

public interface IRVisitor<R> {
    R visitAdd(Add instruction);
    R visitCall(Call instruction);
    R visitDiv(Div instruction);
    R visitEqual(Equal instruction);
    R visitGreater(Greater instruction);
    R visitGreaterEqual(GreaterEqual instruction);
    R visitJmp(Jmp instruction);
    R visitJmpFalse(JmpFalse instruction);
    R visitJmpTrue(JmpTrue instruction);
    R visitLabelNop(LabelNop instruction);
    R visitLess(Less instruction);
    R visitLessEqual(LessEqual instruction);
    R visitLoad(LoadCaptured instruction);
    R visitLoadLocal(LoadLocal instruction);
    R visitMinus(Minus instruction);
    R visitMul(Mul instruction);
    R visitNop(Nop instruction);
    R visitNot(Not instruction);
    R visitPlus(Plus instruction);
    R visitPop(Pop instruction);
    R visitPush(Push instruction);
    R visitReturn(Return instruction);
    R visitStore(StoreCaptured instruction);
    R visitStoreLocal(StoreLocal instruction);
    R visitSub(Sub instruction);
    R visitVoidReturn(VoidReturn instruction);
}
