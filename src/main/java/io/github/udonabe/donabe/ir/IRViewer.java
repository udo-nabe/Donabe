package io.github.udonabe.donabe.ir;

import io.github.udonabe.donabe.ir.instruction.*;
import io.github.udonabe.donabe.runtime.value.FunctionValue;
import io.github.udonabe.donabe.runtime.value.StringValue;

import java.util.List;

public class IRViewer implements IRVisitor<String> {
    public String getIRString(IRProgram program) {
        StringBuilder result = new StringBuilder();
        for (Instruction instruction : program.instructions()) {
            result.append(instruction.accept(this)).append("\n");
        }
        return result.toString();
    }

    @Override
    public String visitAdd(Add instruction) {
        return "add";
    }

    @Override
    public String visitCall(Call instruction) {
        return "call";
    }

    @Override
    public String visitDiv(Div instruction) {
        return "div";
    }

    @Override
    public String visitEqual(Equal instruction) {
        return "equal";
    }

    @Override
    public String visitGreater(Greater instruction) {
        return "greater";
    }

    @Override
    public String visitGreaterEqual(GreaterEqual instruction) {
        return "greater_equal";
    }

    @Override
    public String visitIndex(Index instruction) {
        return "index";
    }

    @Override
    public String visitJmp(Jmp instruction) {
        return "jmp " + instruction.label().name();
    }

    @Override
    public String visitJmpFalse(JmpFalse instruction) {
        return "jmp_false " + instruction.label().name();
    }

    @Override
    public String visitJmpTrue(JmpTrue instruction) {
        return "jmp_true" + instruction.label().name();
    }

    @Override
    public String visitLabelNop(LabelNop instruction) {
        return instruction.label().name() +  ":";
    }

    @Override
    public String visitLess(Less instruction) {
        return "less";
    }

    @Override
    public String visitLessEqual(LessEqual instruction) {
        return "less_equal";
    }

    @Override
    public String visitLoadCaptured(LoadCaptured instruction) {
        return "load_captured #" + instruction.identifierSlot();
    }

    @Override
    public String visitLoadLocal(LoadLocal instruction) {
        return "load_local #" + instruction.identifierSlot();
    }

    @Override
    public String visitMakeList(MakeList instruction) {
        return "make_list " + instruction.size();
    }

    @Override
    public String visitMinus(Minus instruction) {
        return "minus";
    }

    @Override
    public String visitMul(Mul instruction) {
        return "mul";
    }

    @Override
    public String visitNop(Nop instruction) {
        return "nop";
    }

    @Override
    public String visitNot(Not instruction) {
        return "not";
    }

    @Override
    public String visitPlus(Plus instruction) {
        return "plus";
    }

    @Override
    public String visitPop(Pop instruction) {
        return "pop";
    }

    @Override
    public String visitPush(Push instruction) {
        var value = instruction.value();
        if (value instanceof FunctionValue functionValue) {
            List<Instruction> instructions = functionValue.instructions();
            StringBuilder sb = new StringBuilder("push <function>:\n");

            for (Instruction i : instructions) {
                sb.append("  ").append(i.accept(this)).append("\n");
            }

            return sb.toString();
        } else if (value instanceof StringValue(String str)) {
            return "push \"" + str + "\"";
        }
        return "push " + value.display();
    }

    @Override
    public String visitReturn(Return instruction) {
        return "return";
    }

    @Override
    public String visitStoreCaptured(StoreCaptured instruction) {
        return "store_captured #" + instruction.identifierSlot();
    }

    @Override
    public String visitStoreLocal(StoreLocal instruction) {
        return "store_local #" + instruction.identifierSlot();
    }

    @Override
    public String visitSub(Sub instruction) {
        return "sub";
    }

    @Override
    public String visitVoidReturn(VoidReturn instruction) {
        return "vreturn";
    }
}
