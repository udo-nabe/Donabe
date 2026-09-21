package io.github.udonabe.donabe.compile.code;

import io.github.udonabe.donabe.compile.code.constant.MemberRefEntry;
import io.github.udonabe.donabe.compile.code.constant.ValueEntry;
import io.github.udonabe.donabe.compile.code.instruction.ByteCodeInstruction;
import io.github.udonabe.donabe.compile.code.instruction.operand.ConstantPoolOperand;
import io.github.udonabe.donabe.compile.code.instruction.operand.IdentifierSlotOperand;
import io.github.udonabe.donabe.compile.code.instruction.operand.JumpOperand;
import io.github.udonabe.donabe.compile.code.instruction.operand.Operand;
import io.github.udonabe.donabe.compile.code.instruction.operand.SizeOperand;
import io.github.udonabe.donabe.compile.code.section.CodeSection;
import io.github.udonabe.donabe.compile.code.section.ConstantPoolSection;
import io.github.udonabe.donabe.compile.code.value.BoolCodeValue;
import io.github.udonabe.donabe.compile.code.value.CodeValue;
import io.github.udonabe.donabe.compile.code.value.FunctionCodeValue;
import io.github.udonabe.donabe.compile.code.value.IntCodeValue;
import io.github.udonabe.donabe.compile.code.value.ListCodeValue;
import io.github.udonabe.donabe.compile.code.value.StringCodeValue;
import io.github.udonabe.donabe.ir.IRProgram;
import io.github.udonabe.donabe.ir.IRVisitor;
import io.github.udonabe.donabe.ir.instruction.*;
import io.github.udonabe.donabe.ir.instruction.label.Label;
import io.github.udonabe.donabe.ir.value.BooleanValue;
import io.github.udonabe.donabe.ir.value.FunctionValue;
import io.github.udonabe.donabe.ir.value.IntegerValue;
import io.github.udonabe.donabe.ir.value.ListValue;
import io.github.udonabe.donabe.ir.value.RuntimeValue;
import io.github.udonabe.donabe.ir.value.StringValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.udonabe.donabe.compile.code.constant.ConstantPoolEntry;
import io.github.udonabe.donabe.compile.code.constant.TopLevelRefEntry;
import io.github.udonabe.donabe.compile.code.value.Int64CodeValue;
import io.github.udonabe.donabe.ir.value.Int64Value;
import java.util.Set;

public final class ProgramConverter implements IRVisitor<List<Operand>> {

    private final Map<Label, Integer> labelOffsetMap;
    private final List<ConstantPoolEntry<?>> constantPool;

    public ProgramConverter() {
        this.labelOffsetMap = new HashMap<>();
        this.constantPool = new ArrayList<>();
    }

    public ByteCode generate(IRProgram program) {
        var instructions = generate(program.instructions());
        return new ByteCode(Set.of(
                instructions, new ConstantPoolSection(constantPool)
        ));
    }

    private CodeSection generate(List<Instruction> instructions) {
        List<ByteCodeInstruction> result = new ArrayList<>();

        labelOffsetMap.putAll(resolveLabel(instructions));
        for (Instruction instruction : instructions) {
            ByteCodeInstruction i = new ByteCodeInstruction(
                    instruction.opcode(),
                    instruction.accept(this)
            );
            result.add(i);
        }

        return new CodeSection(result);
    }

    @Override
    public List<Operand> visitAdd(Add instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitCall(Call instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitDiv(Div instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitEqual(Equal instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitGreater(Greater instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitGreaterEqual(GreaterEqual instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitIndex(Index instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitJmp(Jmp instruction) {
        return jmp(instruction.label());
    }

    @Override
    public List<Operand> visitJmpFalse(JmpFalse instruction) {
        return jmp(instruction.label());
    }

    @Override
    public List<Operand> visitJmpTrue(JmpTrue instruction) {
        return jmp(instruction.label());
    }

    @Override
    public List<Operand> visitLabelNop(LabelNop instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitLess(Less instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitLessEqual(LessEqual instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitLoadCaptured(LoadCaptured instruction) {
        return List.of(new IdentifierSlotOperand(instruction.identifierSlot()));
    }

    @Override
    public List<Operand> visitLoadLocal(LoadLocal instruction) {
        return List.of(new IdentifierSlotOperand(instruction.identifierSlot()));
    }

    @Override
    public List<Operand> visitLoadMember(LoadMember instruction) {
        return List.of(new ConstantPoolOperand(
                getConstantPoolIndex(new MemberRefEntry(instruction.memberName())
                )
        )
        );
    }

    @Override
    public List<Operand> visitMakeList(MakeList instruction) {
        return List.of(new SizeOperand(instruction.count()));
    }

    @Override
    public List<Operand> visitMinus(Minus instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitMul(Mul instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitNop(Nop instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitNot(Not instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitPlus(Plus instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitPop(Pop instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitPush(Push instruction) {
        return List.of(new ConstantPoolOperand(
                getConstantPoolIndex(new ValueEntry(
                        convertRuntimeValue(instruction.value())
                ))
        ));
    }

    @Override
    public List<Operand> visitReturn(Return instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitStoreCaptured(StoreCaptured instruction) {
        return List.of(new IdentifierSlotOperand(instruction.identifierSlot()));
    }

    @Override
    public List<Operand> visitStoreLocal(StoreLocal instruction) {
        return List.of(new IdentifierSlotOperand(instruction.identifierSlot()));
    }

    @Override
    public List<Operand> visitSub(Sub instruction) {
        return List.of();
    }

    @Override
    public List<Operand> visitVoidReturn(VoidReturn instruction) {
        return List.of();
    }

    private Map<Label, Integer> resolveLabel(List<Instruction> instructions) {
        Map<Label, Integer> result = new HashMap<>();

        int offset = 0;
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);

            if (instruction instanceof LabelNop labelNop) {
                result.put(labelNop.label(), offset);
            }
            offset += instruction.size();
        }

        return Map.copyOf(result);
    }

    private List<Operand> jmp(Label label) {
        Integer index = labelOffsetMap.get(label);
        if (index == null) {
            throw new IllegalStateException("Label '%s' not found.".formatted(label.name()));
        }

        return List.of(new JumpOperand(index));
    }

    private Short getConstantPoolIndex(ConstantPoolEntry<?> value) {
        if (!constantPool.contains(value)) {
            constantPool.add(value);
        }

        if (constantPool.size() > Short.MAX_VALUE) {
            throw new IllegalStateException("Constant pool size is too many.");
        }

        int index = constantPool.indexOf(value);

        return (short) index;
    }

    private CodeValue convertRuntimeValue(RuntimeValue<?> target) {
        return switch (target) {
            case BooleanValue value ->
                new BoolCodeValue(value.value());
            case IntegerValue value ->
                new IntCodeValue(value.value());
            case Int64Value value ->
                new Int64CodeValue(value.value());
            case StringValue value ->
                new StringCodeValue(value.value());
            case FunctionValue value ->
                new FunctionCodeValue(value.name(), value.paramSlots(), value.locals(), generate(value.instructions()));
            case ListValue value ->
                new ListCodeValue(value.value().stream()
                .map(t -> convertRuntimeValue(t))
                .toList());
            default ->
                throw new IllegalStateException("Unexpected value: " + (target));
        };
    }

    @Override
    public List<Operand> visitLoadGlobal(LoadGlobal instruction) {
        return List.of(
                new ConstantPoolOperand(
                        getConstantPoolIndex(new TopLevelRefEntry(instruction.globalName())
                        )
                )
        );
    }

    @Override
    public List<Operand> visitStoreGlobal(StoreGlobal instruction) {
        return List.of(
                new ConstantPoolOperand(
                        getConstantPoolIndex(new TopLevelRefEntry(instruction.globalName())
                        )
                )
        );
    }
}
