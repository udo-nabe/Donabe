package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRProgram;
import io.github.udonabe.donabe.ir.instruction.*;
import io.github.udonabe.donabe.ir.instruction.label.Label;
import io.github.udonabe.donabe.runtime.value.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IRInterpreterTest {
    private void assertStack(List<Instruction> instructions, List<RuntimeValue<?>> stack) {
        assertStack(instructions, stack, Map.of());
    }

    private void assertStack(List<Instruction> instructions, List<RuntimeValue<?>> stack, Map<Integer, VariableCell> slots) {
        var merged = new HashMap<Integer, VariableCell>();

        merged.put(0, new VariableCell(new UndefinedValue()));
        merged.put(1, new VariableCell(new UndefinedValue()));
        merged.put(2, new VariableCell(new UndefinedValue()));
        merged.put(3, new VariableCell(new UndefinedValue()));
        merged.put(4, new VariableCell(new UndefinedValue()));

        merged.putAll(slots);

        IRInterpreter interpreter = new IRInterpreter(new IRProgram(instructions), merged, new Operations());
        interpreter.run();
        assertEquals(
                stack,
                interpreter.snapshotStack()
        );
    }

    private void throwInterpreterException(List<Instruction> instructions) {
        assertThrows(InterpreterException.class, () -> new IRInterpreter(new IRProgram(instructions), Map.of(), new Operations()).run());
    }

    private IRLocation dummyLocation() {
        return new IRLocation(1);
    }

    @Test
    void run() {
    }

    @Test
    void visitAdd() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Add(dummyLocation())
                ),
                List.of(
                        new IntegerValue(43)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new StringValue("1"), dummyLocation()),
                        new Add(dummyLocation())
                ),
                List.of(
                        new StringValue("421")
                )
        );
        assertStack(
                List.of(
                        new Push(new StringValue("42"), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Add(dummyLocation())
                ),
                List.of(
                        new StringValue("421")
                )
        );
        assertStack(
                List.of(
                        new Push(new StringValue("42"), dummyLocation()),
                        new Push(new StringValue("1"), dummyLocation()),
                        new Add(dummyLocation())
                ),
                List.of(
                        new StringValue("421")
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new BooleanValue(true), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Add(dummyLocation())
                )
        );
    }

    @Test
    void visitCall() {
        assertStack(List.of(
                new Push(new StringValue("load-captured"), dummyLocation()),
                new StoreLocal(6, dummyLocation()),
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new Push(new StringValue("inner"), dummyLocation()),
                        new StoreCaptured(6, dummyLocation()),
                        new VoidReturn(dummyLocation())
                )), dummyLocation()),
                new Call(dummyLocation()),
                new Pop(dummyLocation()),
                new LoadLocal(6, dummyLocation())
        ), List.of(
                new StringValue("inner")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        //スタックが引数より多い
        assertStack(List.of(
                new Push(new IntegerValue(3), dummyLocation()),
                new Push(new IntegerValue(1), dummyLocation()),
                new Push(new IntegerValue(2), dummyLocation()),
                new Push(new FunctionValue("add", List.of(6, 7), Set.of(6, 7), List.of(
                        new LoadLocal(6, dummyLocation()),
                        new LoadLocal(7, dummyLocation()),
                        new Add(dummyLocation()),
                        new Return(dummyLocation())
                )), dummyLocation()),
                new Call(dummyLocation())
        ), List.of(
                new IntegerValue(3),
                new IntegerValue(3)
        ), Map.of(
                6, new VariableCell(new UndefinedValue()),
                7, new VariableCell(new UndefinedValue())
        ));

        //引数が足りない
        throwInterpreterException(List.of(
                new Push(new FunctionValue("arg", List.of(7), Set.of(7), List.of(
                        new VoidReturn(dummyLocation())
                )), dummyLocation()),
                new Call(dummyLocation())
        ));
        //関数でない
        throwInterpreterException(List.of(
                new Push(new StringValue("non-callable"), dummyLocation()),
                new Call(dummyLocation())
        ));
    }

    @Test
    void visitDiv() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(3), dummyLocation()),
                        new Div(dummyLocation())
                ),
                List.of(
                        new IntegerValue(14)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(10), dummyLocation()),
                        new Push(new IntegerValue(3), dummyLocation()),
                        new Div(dummyLocation())
                ),
                List.of(
                        new IntegerValue(3)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge"), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Div(dummyLocation())
                )
        );
    }

    @Test
    void visitEqual() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Equal(dummyLocation())
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Equal(dummyLocation())
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new StringValue("42"), dummyLocation()),
                        new Equal(dummyLocation())
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new StringValue("42"), dummyLocation()),
                        new Push(new StringValue("42"), dummyLocation()),
                        new Equal(dummyLocation())
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
    }

    @Test
    void visitGreater() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Greater(dummyLocation())
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Greater(dummyLocation())
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Greater(dummyLocation())
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new StringValue("1"), dummyLocation()),
                        new Greater(dummyLocation())
                )
        );
    }

    @Test
    void visitGreaterEqual() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new GreaterEqual(dummyLocation())
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new GreaterEqual(dummyLocation())
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new GreaterEqual(dummyLocation())
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new StringValue("1"), dummyLocation()),
                        new GreaterEqual(dummyLocation())
                )
        );
    }

    @Test
    void visitJmp() {
        assertStack(
                List.of(
                        new Jmp(new Label(".test"), dummyLocation()),
                        new Nop(dummyLocation()),
                        new Push(new StringValue("no-push"), dummyLocation()),
                        new LabelNop(new Label(".test"), dummyLocation())
                ),
                List.of()
        );
        throwInterpreterException(List.of(
                new Jmp(new Label(".not-found"), dummyLocation())
        ));
    }

    @Test
    void visitJmpFalse() {
        assertStack(
                List.of(
                        new Push(new BooleanValue(false), dummyLocation()),
                        new JmpFalse(new Label(".test"), dummyLocation()),
                        new Nop(dummyLocation()),
                        new Push(new StringValue("no-push"), dummyLocation()),
                        new LabelNop(new Label(".test"), dummyLocation())
                ),
                List.of()
        );
        assertStack(
                List.of(
                        new Push(new BooleanValue(true), dummyLocation()),
                        new JmpFalse(new Label(".test"), dummyLocation()),
                        new Nop(dummyLocation()),
                        new Push(new StringValue("push"), dummyLocation()),
                        new LabelNop(new Label(".test"), dummyLocation())
                ),
                List.of(new StringValue("push"))
        );
        throwInterpreterException(List.of(
                new Jmp(new Label(".not-found"), dummyLocation())
        ));
        throwInterpreterException(List.of(
                new JmpFalse(new Label(".stack-empty"), dummyLocation()),
                new LabelNop(new Label(".stack-empty"), dummyLocation())
        ));
        throwInterpreterException(List.of(
                new Push(new StringValue("string"), dummyLocation()),
                new JmpFalse(new Label(".not-boolean"), dummyLocation()),
                new LabelNop(new Label(".not-boolean"), dummyLocation())
        ));
    }

    @Test
    void visitJmpTrue() {
        assertStack(
                List.of(
                        new Push(new BooleanValue(true), dummyLocation()),
                        new JmpTrue(new Label(".test"), dummyLocation()),
                        new Nop(dummyLocation()),
                        new Push(new StringValue("no-push"), dummyLocation()),
                        new LabelNop(new Label(".test"), dummyLocation())
                ),
                List.of()
        );
        assertStack(
                List.of(
                        new Push(new BooleanValue(false), dummyLocation()),
                        new JmpTrue(new Label(".test"), dummyLocation()),
                        new Nop(dummyLocation()),
                        new Push(new StringValue("push"), dummyLocation()),
                        new LabelNop(new Label(".test"), dummyLocation())
                ),
                List.of(new StringValue("push"))
        );
        throwInterpreterException(List.of(
                new Jmp(new Label(".not-found"), dummyLocation())
        ));
        throwInterpreterException(List.of(
                new JmpFalse(new Label(".stack-empty"), dummyLocation()),
                new LabelNop(new Label(".stack-empty"), dummyLocation())
        ));
        throwInterpreterException(List.of(
                new Push(new StringValue("string"), dummyLocation()),
                new JmpFalse(new Label(".not-boolean"), dummyLocation()),
                new LabelNop(new Label(".not-boolean"), dummyLocation())
        ));
    }

    @Test
    void visitLabelNop() {
        assertStack(List.of(
                new Push(new StringValue("string"), dummyLocation()),
                new LabelNop(new Label(".label"), dummyLocation())
        ), List.of(
                new StringValue("string")
        ));
    }

    @Test
    void visitLess() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Less(dummyLocation())
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Less(dummyLocation())
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Less(dummyLocation())
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new StringValue("1"), dummyLocation()),
                        new Less(dummyLocation())
                )
        );
    }

    @Test
    void visitLessEqual() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new LessEqual(dummyLocation())
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new LessEqual(dummyLocation())
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new LessEqual(dummyLocation())
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new StringValue("1"), dummyLocation()),
                        new LessEqual(dummyLocation())
                )
        );
    }

    @Test
    void visitLoadCaptured() {
        assertStack(List.of(
                new Push(new StringValue("load-captured"), dummyLocation()),
                new StoreLocal(6, dummyLocation()),
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new LoadCaptured(6, dummyLocation()),
                        new Return(dummyLocation())
                )), dummyLocation()),
                new Call(dummyLocation())
        ), List.of(
                new StringValue("load-captured")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        //未定義
        throwInterpreterException(List.of(
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new LoadCaptured(6, dummyLocation())
                )), dummyLocation()),
                new Call(dummyLocation())
        ));
    }

    @Test
    void visitLoadLocal() {
        assertStack(List.of(
                new Push(new StringValue("load-captured"), dummyLocation()),
                new StoreLocal(6, dummyLocation()),
                new LoadLocal(6, dummyLocation())
        ), List.of(
                new StringValue("load-captured")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        throwInterpreterException(List.of(
                new LoadLocal(42, dummyLocation())
        ));
    }

    @Test
    void visitMinus() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Minus(dummyLocation())
                ),
                List.of(
                        new IntegerValue(-42)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(-10), dummyLocation()),
                        new Minus(dummyLocation())
                ),
                List.of(
                        new IntegerValue(10)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge"), dummyLocation()),
                        new Minus(dummyLocation())
                )
        );
    }

    @Test
    void visitMul() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(3), dummyLocation()),
                        new Push(new IntegerValue(3), dummyLocation()),
                        new Mul(dummyLocation())
                ),
                List.of(
                        new IntegerValue(9)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(10), dummyLocation()),
                        new Push(new IntegerValue(-3), dummyLocation()),
                        new Mul(dummyLocation())
                ),
                List.of(
                        new IntegerValue(-30)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge"), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Mul(dummyLocation())
                )
        );
    }

    @Test
    void visitNop() {
        assertStack(List.of(
                new Push(new StringValue("string"), dummyLocation()),
                new Nop(dummyLocation())
        ), List.of(
                new StringValue("string")
        ));
    }

    @Test
    void visitNot() {
        assertStack(
                List.of(
                        new Push(new BooleanValue(true), dummyLocation()),
                        new Not(dummyLocation())
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new BooleanValue(false), dummyLocation()),
                        new Not(dummyLocation())
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge"), dummyLocation()),
                        new Not(dummyLocation())
                )
        );
    }

    @Test
    void visitPlus() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Plus(dummyLocation())
                ),
                List.of(
                        new IntegerValue(42)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(-10), dummyLocation()),
                        new Plus(dummyLocation())
                ),
                List.of(
                        new IntegerValue(-10)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge"), dummyLocation()),
                        new Plus(dummyLocation())
                )
        );
    }

    @Test
    void visitPop() {
        assertStack(List.of(
                new Push(new IntegerValue(1234), dummyLocation()),
                new Push(new IntegerValue(1), dummyLocation()),
                new Pop(dummyLocation())
        ), List.of(
                new IntegerValue(1234)
        ));
    }

    @Test
    void visitPush() {
        assertStack(List.of(
                new Push(new IntegerValue(1234), dummyLocation()),
                new Push(new IntegerValue(1), dummyLocation())
        ), List.of(
                new IntegerValue(1),
                new IntegerValue(1234)
        ));
    }

    @Test
    void visitReturn() {
        assertStack(List.of(
                new Push(new IntegerValue(2), dummyLocation()),
                new Push(new IntegerValue(3), dummyLocation()),
                new Push(new FunctionValue("add", List.of(7, 8), Set.of(7, 8), List.of(
                        new LoadLocal(7, dummyLocation()),
                        new LoadLocal(8, dummyLocation()),
                        new Add(dummyLocation()),
                        new Return(dummyLocation())
                )), dummyLocation()),
                new Call(dummyLocation())
        ), List.of(
                new IntegerValue(5)
        ), Map.of(
                7, new VariableCell(new UndefinedValue()),
                8, new VariableCell(new UndefinedValue())
        ));

        throwInterpreterException(List.of(
                new Push(new IntegerValue(-42), dummyLocation()),
                new Return(dummyLocation())
        ));
    }

    @Test
    void visitStoreCaptured() {
        assertStack(List.of(
                new Push(new StringValue("load-captured"), dummyLocation()),
                new StoreLocal(6, dummyLocation()),
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new Push(new StringValue("inner"), dummyLocation()),
                        new StoreCaptured(6, dummyLocation()),
                        new VoidReturn(dummyLocation())
                )), dummyLocation()),
                new Call(dummyLocation()),
                new Pop(dummyLocation()),
                new LoadLocal(6, dummyLocation())
        ), List.of(
                new StringValue("inner")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        //未定義
        throwInterpreterException(List.of(
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new StoreCaptured(6, dummyLocation())
                )), dummyLocation()),
                new Call(dummyLocation())
        ));
    }

    @Test
    void visitStoreLocal() {
        assertStack(List.of(
                new Push(new StringValue("load-local"), dummyLocation()),
                new StoreLocal(6, dummyLocation()),
                new LoadLocal(6, dummyLocation())
        ), List.of(
                new StringValue("load-local")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        throwInterpreterException(List.of(
                new LoadLocal(42, dummyLocation())
        ));
    }

    @Test
    void visitSub() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(3), dummyLocation()),
                        new Sub(dummyLocation())
                ),
                List.of(
                        new IntegerValue(39)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(3), dummyLocation()),
                        new Push(new IntegerValue(10), dummyLocation()),
                        new Sub(dummyLocation())
                ),
                List.of(
                        new IntegerValue(-7)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge"), dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Sub(dummyLocation())
                )
        );
    }

    @Test
    void visitVoidReturn() {
        assertStack(List.of(
                new Push(new FunctionValue("add", List.of(), Set.of(), List.of(
                        new VoidReturn(dummyLocation())
                )), dummyLocation()),
                new Call(dummyLocation())
        ), List.of(
                new VoidValue()
        ));

        throwInterpreterException(List.of(
                new VoidReturn(dummyLocation())
        ));
    }
}