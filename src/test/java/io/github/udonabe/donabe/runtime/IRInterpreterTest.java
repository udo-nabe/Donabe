package io.github.udonabe.donabe.runtime;

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

    @Test
    void run() {
    }

    @Test
    void visitAdd() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(1)),
                        new Add()
                ),
                List.of(
                        new IntegerValue(43)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new StringValue("1")),
                        new Add()
                ),
                List.of(
                        new StringValue("421")
                )
        );
        assertStack(
                List.of(
                        new Push(new StringValue("42")),
                        new Push(new IntegerValue(1)),
                        new Add()
                ),
                List.of(
                        new StringValue("421")
                )
        );
        assertStack(
                List.of(
                        new Push(new StringValue("42")),
                        new Push(new StringValue("1")),
                        new Add()
                ),
                List.of(
                        new StringValue("421")
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new BooleanValue(true)),
                        new Push(new IntegerValue(1)),
                        new Add()
                )
        );
    }

    @Test
    void visitCall() {
        assertStack(List.of(
                new Push(new StringValue("load-captured")),
                new StoreLocal(6),
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new Push(new StringValue("inner")),
                        new StoreCaptured(6),
                        new VoidReturn()
                ))),
                new Call(),
                new Pop(),
                new LoadLocal(6)
        ), List.of(
                new StringValue("inner")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        //スタックが引数より多い
        assertStack(List.of(
                new Push(new IntegerValue(3)),
                new Push(new IntegerValue(1)),
                new Push(new IntegerValue(2)),
                new Push(new FunctionValue("add", List.of(6, 7), Set.of(6, 7), List.of(
                        new LoadLocal(6),
                        new LoadLocal(7),
                        new Add(),
                        new Return()
                ))),
                new Call()
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
                        new VoidReturn()
                ))),
                new Call()
        ));
        //関数でない
        throwInterpreterException(List.of(
                new Push(new StringValue("non-callable")),
                new Call()
        ));
    }

    @Test
    void visitDiv() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(3)),
                        new Div()
                ),
                List.of(
                        new IntegerValue(14)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(10)),
                        new Push(new IntegerValue(3)),
                        new Div()
                ),
                List.of(
                        new IntegerValue(3)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge")),
                        new Push(new IntegerValue(1)),
                        new Div()
                )
        );
    }

    @Test
    void visitEqual() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(42)),
                        new Equal()
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(1)),
                        new Equal()
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new StringValue("42")),
                        new Equal()
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new StringValue("42")),
                        new Push(new StringValue("42")),
                        new Equal()
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
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(1)),
                        new Greater()
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(1)),
                        new Push(new IntegerValue(42)),
                        new Greater()
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(42)),
                        new Greater()
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new StringValue("1")),
                        new Greater()
                )
        );
    }

    @Test
    void visitGreaterEqual() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(1)),
                        new GreaterEqual()
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(1)),
                        new Push(new IntegerValue(42)),
                        new GreaterEqual()
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(42)),
                        new GreaterEqual()
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new StringValue("1")),
                        new GreaterEqual()
                )
        );
    }

    @Test
    void visitJmp() {
        assertStack(
                List.of(
                        new Jmp(new Label(".test")),
                        new Nop(),
                        new Push(new StringValue("no-push")),
                        new LabelNop(new Label(".test"))
                ),
                List.of()
        );
        throwInterpreterException(List.of(
                new Jmp(new Label(".not-found"))
        ));
    }

    @Test
    void visitJmpFalse() {
        assertStack(
                List.of(
                        new Push(new BooleanValue(false)),
                        new JmpFalse(new Label(".test")),
                        new Nop(),
                        new Push(new StringValue("no-push")),
                        new LabelNop(new Label(".test"))
                ),
                List.of()
        );
        assertStack(
                List.of(
                        new Push(new BooleanValue(true)),
                        new JmpFalse(new Label(".test")),
                        new Nop(),
                        new Push(new StringValue("push")),
                        new LabelNop(new Label(".test"))
                ),
                List.of(new StringValue("push"))
        );
        throwInterpreterException(List.of(
                new Jmp(new Label(".not-found"))
        ));
        throwInterpreterException(List.of(
                new JmpFalse(new Label(".stack-empty")),
                new LabelNop(new Label(".stack-empty"))
        ));
        throwInterpreterException(List.of(
                new Push(new StringValue("string")),
                new JmpFalse(new Label(".not-boolean")),
                new LabelNop(new Label(".not-boolean"))
        ));
    }

    @Test
    void visitJmpTrue() {
        assertStack(
                List.of(
                        new Push(new BooleanValue(true)),
                        new JmpTrue(new Label(".test")),
                        new Nop(),
                        new Push(new StringValue("no-push")),
                        new LabelNop(new Label(".test"))
                ),
                List.of()
        );
        assertStack(
                List.of(
                        new Push(new BooleanValue(false)),
                        new JmpTrue(new Label(".test")),
                        new Nop(),
                        new Push(new StringValue("push")),
                        new LabelNop(new Label(".test"))
                ),
                List.of(new StringValue("push"))
        );
        throwInterpreterException(List.of(
                new Jmp(new Label(".not-found"))
        ));
        throwInterpreterException(List.of(
                new JmpFalse(new Label(".stack-empty")),
                new LabelNop(new Label(".stack-empty"))
        ));
        throwInterpreterException(List.of(
                new Push(new StringValue("string")),
                new JmpFalse(new Label(".not-boolean")),
                new LabelNop(new Label(".not-boolean"))
        ));
    }

    @Test
    void visitLabelNop() {
        assertStack(List.of(
                new Push(new StringValue("string")),
                new LabelNop(new Label(".label"))
        ), List.of(
                new StringValue("string")
        ));
    }

    @Test
    void visitLess() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(1)),
                        new Less()
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(1)),
                        new Push(new IntegerValue(42)),
                        new Less()
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(42)),
                        new Less()
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new StringValue("1")),
                        new Less()
                )
        );
    }

    @Test
    void visitLessEqual() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(1)),
                        new LessEqual()
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(1)),
                        new Push(new IntegerValue(42)),
                        new LessEqual()
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(42)),
                        new LessEqual()
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new StringValue("1")),
                        new LessEqual()
                )
        );
    }

    @Test
    void visitLoadCaptured() {
        assertStack(List.of(
                new Push(new StringValue("load-captured")),
                new StoreLocal(6),
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new LoadCaptured(6),
                        new Return()
                ))),
                new Call()
        ), List.of(
                new StringValue("load-captured")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        //未定義
        throwInterpreterException(List.of(
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new LoadCaptured(6)
                ))),
                new Call()
        ));
    }

    @Test
    void visitLoadLocal() {
        assertStack(List.of(
                new Push(new StringValue("load-captured")),
                new StoreLocal(6),
                new LoadLocal(6)
        ), List.of(
                new StringValue("load-captured")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        throwInterpreterException(List.of(
                new LoadLocal(42)
        ));
    }

    @Test
    void visitMinus() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Minus()
                ),
                List.of(
                        new IntegerValue(-42)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(-10)),
                        new Minus()
                ),
                List.of(
                        new IntegerValue(10)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge")),
                        new Minus()
                )
        );
    }

    @Test
    void visitMul() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(3)),
                        new Push(new IntegerValue(3)),
                        new Mul()
                ),
                List.of(
                        new IntegerValue(9)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(10)),
                        new Push(new IntegerValue(-3)),
                        new Mul()
                ),
                List.of(
                        new IntegerValue(-30)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge")),
                        new Push(new IntegerValue(1)),
                        new Mul()
                )
        );
    }

    @Test
    void visitNop() {
        assertStack(List.of(
                new Push(new StringValue("string")),
                new Nop()
        ), List.of(
                new StringValue("string")
        ));
    }

    @Test
    void visitNot() {
        assertStack(
                List.of(
                        new Push(new BooleanValue(true)),
                        new Not()
                ),
                List.of(
                        new BooleanValue(false)
                )
        );
        assertStack(
                List.of(
                        new Push(new BooleanValue(false)),
                        new Not()
                ),
                List.of(
                        new BooleanValue(true)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge")),
                        new Not()
                )
        );
    }

    @Test
    void visitPlus() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Plus()
                ),
                List.of(
                        new IntegerValue(42)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(-10)),
                        new Plus()
                ),
                List.of(
                        new IntegerValue(-10)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge")),
                        new Plus()
                )
        );
    }

    @Test
    void visitPop() {
        assertStack(List.of(
                new Push(new IntegerValue(1234)),
                new Push(new IntegerValue(1)),
                new Pop()
        ), List.of(
                new IntegerValue(1234)
        ));
    }

    @Test
    void visitPush() {
        assertStack(List.of(
                new Push(new IntegerValue(1234)),
                new Push(new IntegerValue(1))
        ), List.of(
                new IntegerValue(1),
                new IntegerValue(1234)
        ));
    }

    @Test
    void visitReturn() {
        assertStack(List.of(
                new Push(new IntegerValue(2)),
                new Push(new IntegerValue(3)),
                new Push(new FunctionValue("add", List.of(7, 8), Set.of(7, 8), List.of(
                        new LoadLocal(7),
                        new LoadLocal(8),
                        new Add(),
                        new Return()
                ))),
                new Call()
        ), List.of(
                new IntegerValue(5)
        ), Map.of(
                7, new VariableCell(new UndefinedValue()),
                8, new VariableCell(new UndefinedValue())
        ));

        throwInterpreterException(List.of(
                new Push(new IntegerValue(-42)),
                new Return()
        ));
    }

    @Test
    void visitStoreCaptured() {
        assertStack(List.of(
                new Push(new StringValue("load-captured")),
                new StoreLocal(6),
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new Push(new StringValue("inner")),
                        new StoreCaptured(6),
                        new VoidReturn()
                ))),
                new Call(),
                new Pop(),
                new LoadLocal(6)
        ), List.of(
                new StringValue("inner")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        //未定義
        throwInterpreterException(List.of(
                new Push(new FunctionValue("captured", List.of(), Set.of(), List.of(
                        new StoreCaptured(6)
                ))),
                new Call()
        ));
    }

    @Test
    void visitStoreLocal() {
        assertStack(List.of(
                new Push(new StringValue("load-local")),
                new StoreLocal(6),
                new LoadLocal(6)
        ), List.of(
                new StringValue("load-local")
        ), Map.of(
                6, new VariableCell(new UndefinedValue())
        ));

        throwInterpreterException(List.of(
                new LoadLocal(42)
        ));
    }

    @Test
    void visitSub() {
        assertStack(
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(3)),
                        new Sub()
                ),
                List.of(
                        new IntegerValue(39)
                )
        );
        assertStack(
                List.of(
                        new Push(new IntegerValue(3)),
                        new Push(new IntegerValue(10)),
                        new Sub()
                ),
                List.of(
                        new IntegerValue(-7)
                )
        );
        throwInterpreterException(
                List.of(
                        new Push(new StringValue("hoge")),
                        new Push(new IntegerValue(1)),
                        new Sub()
                )
        );
    }

    @Test
    void visitVoidReturn() {
        assertStack(List.of(
                new Push(new FunctionValue("add", List.of(), Set.of(), List.of(
                        new VoidReturn()
                ))),
                new Call()
        ), List.of(
                new VoidValue()
        ));

        throwInterpreterException(List.of(
                new VoidReturn()
        ));
    }
}