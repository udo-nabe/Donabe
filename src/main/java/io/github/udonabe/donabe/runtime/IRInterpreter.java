package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.ast.expr.BinaryOperator;
import io.github.udonabe.donabe.ast.expr.UnaryOperator;
import io.github.udonabe.donabe.ir.IRProgram;
import io.github.udonabe.donabe.ir.IRVisitor;
import io.github.udonabe.donabe.ir.instruction.*;
import io.github.udonabe.donabe.ir.instruction.label.Label;
import io.github.udonabe.donabe.runtime.context.InterpretContext;
import io.github.udonabe.donabe.runtime.context.stack.StackFrame;
import io.github.udonabe.donabe.runtime.value.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class IRInterpreter implements IRVisitor<Void> {
    private static final Logger log = LoggerFactory.getLogger(IRInterpreter.class);

    private final InterpretContext context;
    private final Operations registry;
    private final Map<Label, Integer> labelJmpMap;

    public IRInterpreter(IRProgram program, Set<Integer> resolution, Operations registry) {
        this.context = new InterpretContext(program.instructions(), resolution);
        this.registry = registry;
        this.labelJmpMap = new HashMap<>();
        log.debug("resolution: {}", resolution);
    }

    private void declareBuiltinFunctions() {
        context.setLocalVarValue(0, BuiltinFunctions.BUILTIN_PRINT);
        context.setLocalVarValue(1, BuiltinFunctions.BUILTIN_INPUT);
        context.setLocalVarValue(2, BuiltinFunctions.BUILTIN_RANGE);
    }

    private void setupLabel(List<Instruction> instructions) {
        List<LabelNop> labels = instructions.stream()
                .filter(i -> i instanceof LabelNop)
                .map(i -> (LabelNop) i)
                .toList();
        for (LabelNop label : labels) {
            int labelPC = instructions.indexOf(label);
            labelJmpMap.put(label.label(), labelPC);
            log.debug("Register label {}, PC={}", label.label(), labelPC);
        }
    }

    public void run() {
        setupLabel(context.peekStackFrame().instructions());
        declareBuiltinFunctions();
        while (!context.isFinished()) {
            Instruction fetched = context.currentInstruction();
            context.incrementPC();
            fetched.accept(this);
        }
    }

    @Override
    public Void visitAdd(Add instruction) {
        RuntimeValue<?> rhs = context.popStack();
        RuntimeValue<?> lhs = context.popStack();
        RuntimeValue<?> result = registry.applyBinary(BinaryOperator.PLUS, lhs, rhs, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    private List<RuntimeValue<?>> bindArgs(int formalArgsSize) {
        List<RuntimeValue<?>> result = new ArrayList<>();
        for (int i = 0; i < formalArgsSize; i++) {
            try {
                result.add(context.popStack());
            } catch (InterpreterException e) {
                throw new InterpreterException("The lengths of the actual argument list and the formal argument list differ.", context.peekStackFrame());
            }
        }
        return result;
    }

    private ClosureValue setupClosure(FunctionValue functionValue) {
        ClosureValue closure = new ClosureValue(functionValue.name(),
                functionValue.paramSlots(),
                functionValue.locals(),
                functionValue.instructions(),
                context.peekStackFrame());
        setupLabel(closure.instructions());
        return closure;
    }

    @Override
    public Void visitCall(Call instruction) {
        RuntimeValue<?> callee = context.popStack();
        switch (callee) {
            case ClosureValue(
                    String name, List<Integer> paramSlots, Set<Integer> locals, List<Instruction> instructions,
                    StackFrame parent
            ) -> {
                List<RuntimeValue<?>> argValues = bindArgs(paramSlots.size());

                StackFrame calleeStackFrame = new StackFrame(context.peekStackFrame(), parent, name, instructions, locals);

                context.pushStackFrame(calleeStackFrame);

                for (int i = 0; i < paramSlots.size(); i++) {
                    int argSlot = paramSlots.get(i);
                    RuntimeValue<?> argValue = argValues.get(i);
                    context.setLocalVarValue(argSlot, argValue);
                }
            }
            case BuiltinFunctionValue(
                    List<String> formalArgs, Function<List<? extends RuntimeValue<?>>, RuntimeValue<?>> content
            ) -> {
                try {
                    List<RuntimeValue<?>> args = bindArgs(formalArgs.size());
                    RuntimeValue<?> returnValue = content.apply(args);
                    context.pushStack(returnValue);
                } catch (InterpreterException e) {
                    throw new InterpreterException(e.getMessage(), e, e.occurredFrame() == null ? context.peekStackFrame() : e.occurredFrame());
                }
            }
            default -> {
                throw new InterpreterException("The callee is not callable. callee: %s", context.peekStackFrame());
            }
        }

        return null;
    }

    @Override
    public Void visitDiv(Div instruction) {
        RuntimeValue<?> rhs = context.popStack();
        RuntimeValue<?> lhs = context.popStack();
        RuntimeValue<?> result = registry.applyBinary(BinaryOperator.DIVISION, lhs, rhs, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitEqual(Equal instruction) {
        RuntimeValue<?> rhs = context.popStack();
        RuntimeValue<?> lhs = context.popStack();
        RuntimeValue<?> result = registry.applyBinary(BinaryOperator.EQUAL, lhs, rhs, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitGreater(Greater instruction) {
        RuntimeValue<?> rhs = context.popStack();
        RuntimeValue<?> lhs = context.popStack();
        RuntimeValue<?> result = registry.applyBinary(BinaryOperator.GREATER, lhs, rhs, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitGreaterEqual(GreaterEqual instruction) {
        RuntimeValue<?> rhs = context.popStack();
        RuntimeValue<?> lhs = context.popStack();
        RuntimeValue<?> result = registry.applyBinary(BinaryOperator.GREATER_EQUAL, lhs, rhs, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitIndex(Index instruction) {
        RuntimeValue<?> index = context.popStack();
        RuntimeValue<?> target = context.popStack();

        if (!(index instanceof IntegerValue i)) {
            throw new InterpreterException("Index must be integer.", context.peekStackFrame());
        }
        if (!(target instanceof ListValue list)) {
            throw new InterpreterException("The target of index must be list.", context.peekStackFrame());
        }
        RuntimeValue<?> result = list.value().get(i.value());
        context.pushStack(result);
        return null;
    }

    private int getLabelPC(Label label) {
        if (!labelJmpMap.containsKey(label)) {
            throw new InterpreterException("Undefined label: " + label.name(), context.peekStackFrame());
        }
        return labelJmpMap.get(label);
    }

    @Override
    public Void visitJmp(Jmp instruction) {
        int jmpTo = getLabelPC(instruction.label());
        context.peekStackFrame().registers().setPC(jmpTo);
        return null;
    }

    @Override
    public Void visitJmpFalse(JmpFalse instruction) {
        RuntimeValue<?> condition = context.popStack();
        if (!(condition instanceof BooleanValue(Boolean value))) {
            throw new InterpreterException("Condition of jmp_false must be type of boolean.", context.peekStackFrame());
        }

        if (!value) {
            int jmpTo = getLabelPC(instruction.label());
            context.peekStackFrame().registers().setPC(jmpTo);
        }
        return null;
    }

    @Override
    public Void visitJmpTrue(JmpTrue instruction) {
        RuntimeValue<?> condition = context.popStack();
        if (!(condition instanceof BooleanValue(Boolean value))) {
            throw new InterpreterException("Condition of jmp_true must be type of boolean.", context.peekStackFrame());
        }

        if (value) {
            int jmpTo = getLabelPC(instruction.label());
            context.peekStackFrame().registers().setPC(jmpTo);
        }
        return null;
    }

    @Override
    public Void visitLabelNop(LabelNop instruction) {
        return null;
    }

    @Override
    public Void visitLess(Less instruction) {
        RuntimeValue<?> rhs = context.popStack();
        RuntimeValue<?> lhs = context.popStack();
        RuntimeValue<?> result = registry.applyBinary(BinaryOperator.LESS, lhs, rhs, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitLessEqual(LessEqual instruction) {
        RuntimeValue<?> rhs = context.popStack();
        RuntimeValue<?> lhs = context.popStack();
        RuntimeValue<?> result = registry.applyBinary(BinaryOperator.LESS_EQUAL, lhs, rhs, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitLoadCaptured(LoadCaptured instruction) {
        RuntimeValue<?> loaded = context.getCaptured(instruction.identifierSlot()).value();

        if (loaded instanceof FunctionValue functionValue) {
            context.pushStack(setupClosure(functionValue));
        } else {
            context.pushStack(loaded);
        }
        return null;
    }

    @Override
    public Void visitLoadLocal(LoadLocal instruction) {
        RuntimeValue<?> loaded = context.getLocal(instruction.identifierSlot()).value();

        if (loaded instanceof FunctionValue functionValue) {
            context.pushStack(setupClosure(functionValue));
        } else {
            context.pushStack(loaded);
        }
        return null;
    }

    @Override
    public Void visitLoadMember(LoadMember instruction) {
        RuntimeValue<?> target = context.popStack();
        context.pushStack(target.findMember(instruction.memberName(), context.peekStackFrame()));
        return null;
    }

    @Override
    public Void visitMakeList(MakeList instruction) {
        int count = instruction.size();

        List<RuntimeValue<?>> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(context.popStack());
        }
        context.pushStack(new ListValue(result.reversed()));
        return null;
    }

    @Override
    public Void visitMinus(Minus instruction) {
        RuntimeValue<?> target = context.popStack();
        RuntimeValue<?> result = registry.applyUnary(UnaryOperator.MINUS, target, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitMul(Mul instruction) {
        RuntimeValue<?> rhs = context.popStack();
        RuntimeValue<?> lhs = context.popStack();
        RuntimeValue<?> result = registry.applyBinary(BinaryOperator.MULTIPLICATION, lhs, rhs, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitNop(Nop instruction) {
        return null;
    }

    @Override
    public Void visitNot(Not instruction) {
        RuntimeValue<?> target = context.popStack();
        RuntimeValue<?> result = registry.applyUnary(UnaryOperator.NOT, target, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitPlus(Plus instruction) {
        RuntimeValue<?> target = context.popStack();
        RuntimeValue<?> result = registry.applyUnary(UnaryOperator.PLUS, target, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitPop(Pop instruction) {
        context.popStack();
        return null;
    }

    @Override
    public Void visitPush(Push instruction) {
        RuntimeValue<?> value = instruction.value();
        if (value instanceof FunctionValue functionValue) {
            context.pushStack(setupClosure(functionValue));
        } else {
            context.pushStack(instruction.value());
        }
        return null;
    }

    @Override
    public Void visitReturn(Return instruction) {
        RuntimeValue<?> returnValue = context.popStack();
        context.popStackFrame();
        context.pushStack(returnValue);
        return null;
    }

    @Override
    public Void visitStoreCaptured(StoreCaptured instruction) {
        RuntimeValue<?> value = context.popStack();
        context.setCapturedVarValue(instruction.identifierSlot(), value);
        return null;
    }

    @Override
    public Void visitStoreLocal(StoreLocal instruction) {
        RuntimeValue<?> value = context.popStack();
        context.setLocalVarValue(instruction.identifierSlot(), value);
        return null;
    }

    @Override
    public Void visitSub(Sub instruction) {
        RuntimeValue<?> rhs = context.popStack();
        RuntimeValue<?> lhs = context.popStack();
        RuntimeValue<?> result = registry.applyBinary(BinaryOperator.MINUS, lhs, rhs, context.peekStackFrame());
        context.pushStack(result);
        return null;
    }

    @Override
    public Void visitVoidReturn(VoidReturn instruction) {
        context.popStackFrame();
        context.pushStack(new VoidValue());
        return null;
    }

    List<RuntimeValue<?>> snapshotStack() {
        return context.snapshotStack();
    }

    List<Instruction> snapshotInstructions() {
        return List.copyOf(context.peekStackFrame().instructions());
    }

    int getPC() {
        return context.pc();
    }
}
