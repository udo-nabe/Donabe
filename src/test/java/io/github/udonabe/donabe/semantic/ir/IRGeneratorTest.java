package io.github.udonabe.donabe.semantic.ir;

import io.github.udonabe.donabe.ast.ASTNode;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ir.instruction.*;
import io.github.udonabe.donabe.ir.instruction.label.Label;
import io.github.udonabe.donabe.runtime.value.BooleanValue;
import io.github.udonabe.donabe.runtime.value.FunctionValue;
import io.github.udonabe.donabe.runtime.value.IntegerValue;
import io.github.udonabe.donabe.runtime.value.StringValue;
import io.github.udonabe.donabe.semantic.Scope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IRGeneratorTest {
    private static final int FIRST_IDENTIFIER_ID = 6;

    private Scope generateSimpleScope(String... identifiers) {
        Scope result = Scope.generateRoot();
        for (int i = 0; i < identifiers.length; i++) {
            String identifier = identifiers[i];
            result.putId(identifier, FIRST_IDENTIFIER_ID + i);
        }
        return result;
    }

    private void assertIRSimple(ASTNode ast, List<Instruction> instructions) {
        assertIR(ast, Scope.generateRoot(), instructions);
    }

    private void assertIR(ASTNode ast, Scope root, List<Instruction> instructions) {
        assertIR(ast, root, instructions, new HashMap<>());
    }

    private void assertIR(ASTNode ast, Scope root, List<Instruction> instructions, Map<ASTNode, Set<Integer>> map) {
        List<Instruction> actualInstructions = ast.accept(new IRGenerator(root, map));
        assertIterableEquals(instructions, actualInstructions);
    }

    @Test
    void visitProgram() {
        assertIR(
                new Program(
                        List.of(
                                new LetDeclaration(
                                        new Identifier("foo", new SourceFileLocation(1, 1)),
                                        new IntegerLiteral(42, new SourceFileLocation(1, 1)),
                                        new SourceFileLocation(1, 1)
                                ),
                                new VarDeclaration(
                                        new Identifier("bar", new SourceFileLocation(1, 1)),
                                        new BinaryExpression(
                                                new Identifier("foo", new SourceFileLocation(1, 1)),
                                                BinaryOperator.PLUS,
                                                new IntegerLiteral(1, new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        ),
                                        new SourceFileLocation(1, 1)
                                ),
                                new ExpressionStatement(
                                        new BinaryExpression(
                                                new Identifier("foo", new SourceFileLocation(1, 1)),
                                                BinaryOperator.MINUS,
                                                new Identifier("bar", new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        ),
                                        new SourceFileLocation(1, 1)
                                )
                        ),
                        new SourceFileLocation(1, 1)
                ),
                generateSimpleScope("foo", "bar"),
                List.of(
                        new Push(new IntegerValue(42)),
                        new Store(6),
                        new Load(6),
                        new Push(new IntegerValue(1)),
                        new Add(),
                        new Store(7),
                        new Load(6),
                        new Load(7),
                        new Sub(),
                        new Pop()
                )
        );
    }

    @Test
    void visitBlockStatement() {
        Scope root = Scope.generateRoot();
        Scope child = root.newChild();
        child.putId("foo", 6);
        child.putId("bar", 7);

        assertIR(
                new BlockStatement(
                        List.of(
                                new LetDeclaration(
                                        new Identifier("foo", new SourceFileLocation(1, 1)),
                                        new IntegerLiteral(42, new SourceFileLocation(1, 1)),
                                        new SourceFileLocation(1, 1)
                                ),
                                new VarDeclaration(
                                        new Identifier("bar", new SourceFileLocation(1, 1)),
                                        new BinaryExpression(
                                                new Identifier("foo", new SourceFileLocation(1, 1)),
                                                BinaryOperator.PLUS,
                                                new IntegerLiteral(1, new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        ),
                                        new SourceFileLocation(1, 1)
                                ),
                                new ExpressionStatement(
                                        new BinaryExpression(
                                                new Identifier("foo", new SourceFileLocation(1, 1)),
                                                BinaryOperator.MINUS,
                                                new Identifier("bar", new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        ),
                                        new SourceFileLocation(1, 1)
                                )
                        ),
                        new SourceFileLocation(1, 1)
                ), root,
                List.of(
                        new Push(new IntegerValue(42)),
                        new Store(6),
                        new Load(6),
                        new Push(new IntegerValue(1)),
                        new Add(),
                        new Store(7),
                        new Load(6),
                        new Load(7),
                        new Sub(),
                        new Pop()
                )
        );
    }

    @Test
    void visitEmptyStatement() {
        assertIRSimple(
                new EmptyStatement(new SourceFileLocation(1, 1)),
                List.of()
        );
    }

    @Test
    void visitExpressionStatement() {
        assertIRSimple(
                new ExpressionStatement(
                        new IntegerLiteral(123, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(new IntegerValue(123)),
                        new Pop()
                )
        );
    }

    @Test
    void functionDefineStatement() {
        Scope root = Scope.generateRoot();

        root.putId("add", 6);
        root.putId("answer", 7);

        Scope function = root.newChild();

        function.putId("a", 8);
        function.putId("b", 9);

        FunctionValue functionValue = new FunctionValue(
                "add",
                List.of(8, 9),
                List.of(
                        new Load(7),
                        new Pop(),
                        new LoadLocal(8),
                        new LoadLocal(9),
                        new Add(),
                        new Return()
                )
        );

        var define = new FunctionDefineStatement(
                new Identifier("add", new SourceFileLocation(1, 1)),
                List.of(
                        new Identifier("a", new SourceFileLocation(1, 1)),
                        new Identifier("b", new SourceFileLocation(1, 1))
                ),
                new BlockStatement(
                        List.of(
                                new ExpressionStatement(
                                        new Identifier("answer", new SourceFileLocation(1, 1)),
                                        new SourceFileLocation(1, 1)
                                ),
                                new ReturnStatement(
                                        new BinaryExpression(
                                                new Identifier("a", new SourceFileLocation(1, 1)),
                                                BinaryOperator.PLUS,
                                                new Identifier("b", new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        ), new SourceFileLocation(1, 1)
                                )
                        ),
                        new SourceFileLocation(1, 1)
                ),
                new SourceFileLocation(1, 1)
        );

        assertIR(
                new Program(
                        List.of(
                                new LetDeclaration(
                                        new Identifier("answer", new SourceFileLocation(1, 1)),
                                        new IntegerLiteral(42, new SourceFileLocation(1, 1)),
                                        new SourceFileLocation(1, 1)
                                ),
                                define
                        ),
                        new SourceFileLocation(1, 1)
                ),
                root,
                List.of(
                        new Push(functionValue),
                        new Store(6),
                        new Push(new IntegerValue(42)),
                        new Store(7)
                ),
                Map.of(
                        define, Set.of(8, 9)
                )
        );
    }

    @Test
    void visitIfStatement() {
        //if-elseがそろっている場合
        Scope root = Scope.generateRoot();

        root.newChild();    //if block
        root.newChild();    //else block

        assertIR(
                new IfStatement(
                        new BooleanLiteral(true, new SourceFileLocation(1, 1)),
                        new BlockStatement(
                                List.of(
                                        new ExpressionStatement(
                                                new IntegerLiteral(1, new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        )
                                ), new SourceFileLocation(1, 1)
                        ),
                        new BlockStatement(
                                List.of(
                                        new ExpressionStatement(
                                                new IntegerLiteral(3, new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        )
                                ), new SourceFileLocation(1, 1)
                        ),
                        new SourceFileLocation(1, 1)
                ),
                root,
                List.of(
                        new Push(new BooleanValue(true)),
                        new JmpFalse(new Label(".0")),

                        new Push(new IntegerValue(1)),
                        new Pop(),
                        new Jmp(new Label(".1")),

                        new LabelNop(new Label(".0")),
                        new Push(new IntegerValue(3)),
                        new Pop(),
                        new Jmp(new Label(".1")),

                        new LabelNop(new Label(".1"))
                )
        );

        //ifのみの場合
        Scope rootOnlyIf = Scope.generateRoot();

        rootOnlyIf.newChild();    //if block

        assertIR(
                new IfStatement(
                        new BooleanLiteral(true, new SourceFileLocation(1, 1)),
                        new BlockStatement(
                                List.of(
                                        new ExpressionStatement(
                                                new IntegerLiteral(1, new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        )
                                ), new SourceFileLocation(1, 1)
                        ),
                        null,
                        new SourceFileLocation(1, 1)
                ),
                rootOnlyIf,
                List.of(
                        new Push(new BooleanValue(true)),
                        new JmpFalse(new Label(".0")),

                        new Push(new IntegerValue(1)),
                        new Pop(),

                        new LabelNop(new Label(".0"))
                )
        );
    }

    @Test
    void visitLetDeclaration() {
        assertIR(
                new LetDeclaration(
                        new Identifier("bar", new SourceFileLocation(1, 1)),
                        new StringLiteral("Hello", new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                generateSimpleScope("bar"),
                List.of(
                        new Push(new StringValue("Hello")),
                        new Store(6)
                )
        );
    }

    @Test
    void visitReturnStatement() {
        //値がある場合
        assertIRSimple(
                new ReturnStatement(
                        new IntegerLiteral(1234, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(new IntegerValue(1234)),
                        new Return()
                )
        );

        //値が無い場合(VoidExpression)
        assertIRSimple(
                new ReturnStatement(
                        new VoidExpression(new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new VoidReturn()
                )
        );
    }

    @Test
    void visitVarDeclaration() {
        assertIR(
                new VarDeclaration(
                        new Identifier("hoge", new SourceFileLocation(1, 1)),
                        new StringLiteral("Hello", new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                generateSimpleScope("hoge"),
                List.of(
                        new Push(new StringValue("Hello")),
                        new Store(6)
                )
        );
    }

    @Test
    void visitWhileStatement() {
        Scope root = Scope.generateRoot();
        root.newChild();

        assertIR(
                new WhileStatement(
                        new BooleanLiteral(true, new SourceFileLocation(1, 1)),
                        new BlockStatement(
                                List.of(
                                        new ExpressionStatement(
                                                new IntegerLiteral(42, new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        )
                                ), new SourceFileLocation(1, 1)
                        ), new SourceFileLocation(1, 1)
                ),
                root,
                List.of(
                        new LabelNop(new Label(".0")),

                        new Push(new BooleanValue(true)),
                        new JmpFalse(new Label(".1")),

                        new Push(new IntegerValue(42)),
                        new Pop(),

                        new Jmp(new Label(".0")),

                        new LabelNop(new Label(".1"))
                )
        );
    }

    @Test
    void visitForEachStatement() {
        //まだ未実装
    }

    @Test
    void visitAssignExpression() {
        assertIR(
                new AssignExpression(
                        new Identifier("bar", new SourceFileLocation(1, 1)),
                        new StringLiteral("Hello", new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                generateSimpleScope("bar"),
                List.of(
                        new Push(new StringValue("Hello")),
                        new Store(6),
                        new Load(6)
                )
        );
    }

    private void binaryExpression(BinaryOperator operator) {
        Instruction operatorInstruction = switch (operator) {
            case PLUS -> new Add();
            case MINUS -> new Sub();
            case MULTIPLICATION -> new Mul();
            case DIVISION -> new Div();
            case EQUAL -> new Equal();
            case LESS -> new Less();
            case GREATER -> new Greater();
            case LESS_EQUAL -> new LessEqual();
            case GREATER_EQUAL -> new GreaterEqual();
        };

        assertIRSimple(
                new BinaryExpression(
                        new IntegerLiteral(42, new SourceFileLocation(1, 1)),
                        operator,
                        new IntegerLiteral(3, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(new IntegerValue(42)),
                        new Push(new IntegerValue(3)),
                        operatorInstruction
                )
        );
    }

    @Test
    void visitBinaryExpression() {
        binaryExpression(BinaryOperator.PLUS);
        binaryExpression(BinaryOperator.MINUS);
        binaryExpression(BinaryOperator.MULTIPLICATION);
        binaryExpression(BinaryOperator.DIVISION);
        binaryExpression(BinaryOperator.EQUAL);
        binaryExpression(BinaryOperator.LESS);
        binaryExpression(BinaryOperator.GREATER);
        binaryExpression(BinaryOperator.LESS_EQUAL);
        binaryExpression(BinaryOperator.GREATER_EQUAL);
    }

    @Test
    void visitBooleanLiteral() {
        assertIRSimple(new BooleanLiteral(true, new SourceFileLocation(1, 1)),
                List.of(
                        new Push(new BooleanValue(true))
                ));
    }

    @Test
    void visitCallExpression() {
        Scope root = Scope.generateRoot();

        root.putId("add", 6);

        Scope function = root.newChild();

        function.putId("a", 7);
        function.putId("b", 8);

        FunctionValue functionValue = new FunctionValue(
                "add",
                List.of(7, 8),
                List.of(
                        new LoadLocal(7),
                        new LoadLocal(8),
                        new Add(),
                        new Return()
                )
        );

        var define = new FunctionDefineStatement(
                new Identifier("add", new SourceFileLocation(1, 1)),
                List.of(
                        new Identifier("a", new SourceFileLocation(1, 1)),
                        new Identifier("b", new SourceFileLocation(1, 1))
                ),
                new BlockStatement(
                        List.of(
                                new ReturnStatement(
                                        new BinaryExpression(
                                                new Identifier("a", new SourceFileLocation(1, 1)),
                                                BinaryOperator.PLUS,
                                                new Identifier("b", new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        ), new SourceFileLocation(1, 1)
                                )
                        ),
                        new SourceFileLocation(1, 1)
                ),
                new SourceFileLocation(1, 1)
        );

        assertIR(
                new Program(
                        List.of(
                                define,
                                new ExpressionStatement(
                                        new CallExpression(
                                                new Identifier("add", new SourceFileLocation(1, 1)),
                                                List.of(
                                                        new IntegerLiteral(123, new SourceFileLocation(1, 1)),
                                                        new IntegerLiteral(1234, new SourceFileLocation(1, 1))
                                                ),
                                                new SourceFileLocation(1, 1)
                                        ),
                                        new SourceFileLocation(1, 1)
                                )
                        ),
                        new SourceFileLocation(1, 1)
                ),
                root,
                List.of(
                        new Push(functionValue),
                        new Store(6),
                        new Push(new IntegerValue(1234)),
                        new Push(new IntegerValue(123)),
                        new Load(6),
                        new Call(),
                        new Pop()
                ),
                Map.of(
                        define, Set.of(7, 8)
                )
        );
    }

    @Test
    void visitCompoundAssignExpression() {
        assertIR(
                new CompoundAssignExpression(
                        new Identifier("bar", new SourceFileLocation(1, 1)),
                        CompoundAssignOperator.PLUS,
                        new IntegerLiteral(1234, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                generateSimpleScope("bar"),
                List.of(
                        new Load(6),
                        new Push(new IntegerValue(1234)),
                        new Add(),
                        new Store(6),
                        new Load(6)
                )
        );
    }

    @Test
    void visitDecrement() {
        //前置
        assertIR(
                new Decrement(
                        new Identifier("hoge", new SourceFileLocation(1, 1)),
                        true,
                        new SourceFileLocation(1, 1)
                ),
                generateSimpleScope("hoge"),
                List.of(
                        new Load(6),
                        new Push(new IntegerValue(1)),
                        new Sub(),
                        new Store(6),
                        new Load(6)
                )
        );

        //後置
        assertIR(
                new Decrement(
                        new Identifier("hoge", new SourceFileLocation(1, 1)),
                        false,
                        new SourceFileLocation(1, 1)
                ),
                generateSimpleScope("hoge"),
                List.of(
                        new Load(6),
                        new Load(6),
                        new Push(new IntegerValue(1)),
                        new Sub(),
                        new Store(6)
                )
        );
    }

    @Test
    void visitFunctionLiteral() {
        Scope root = Scope.generateRoot();

        Scope function = root.newChild();

        function.putId("a", 6);
        function.putId("b", 7);

        FunctionValue functionValue = new FunctionValue(
                null,
                List.of(6, 7),
                List.of(
                        new LoadLocal(6),
                        new LoadLocal(7),
                        new Add(),
                        new Return()
                )
        );

        var define = new FunctionLiteral(
                List.of(
                        new Identifier("a", new SourceFileLocation(1, 1)),
                        new Identifier("b", new SourceFileLocation(1, 1))
                ),
                new BlockStatement(
                        List.of(
                                new ReturnStatement(
                                        new BinaryExpression(
                                                new Identifier("a", new SourceFileLocation(1, 1)),
                                                BinaryOperator.PLUS,
                                                new Identifier("b", new SourceFileLocation(1, 1)),
                                                new SourceFileLocation(1, 1)
                                        ), new SourceFileLocation(1, 1)
                                )
                        ),
                        new SourceFileLocation(1, 1)
                ),
                new SourceFileLocation(1, 1)
        );

        assertIR(
                define,
                root,
                List.of(
                        new Push(functionValue)
                ),
                Map.of(
                        define, Set.of(6, 7)
                )
        );
    }

    @Test
    void visitIdentifier() {
        assertIR(
                new Identifier("foo", new SourceFileLocation(1, 1)),
                generateSimpleScope("foo"),
                List.of(
                        new Load(6)
                )
        );
    }

    @Test
    void visitIncrement() {
        //前置
        assertIR(
                new Increment(
                        new Identifier("hoge", new SourceFileLocation(1, 1)),
                        true,
                        new SourceFileLocation(1, 1)
                ),
                generateSimpleScope("hoge"),
                List.of(
                        new Load(6),
                        new Push(new IntegerValue(1)),
                        new Add(),
                        new Store(6),
                        new Load(6)
                )
        );

        //後置
        assertIR(
                new Increment(
                        new Identifier("hoge", new SourceFileLocation(1, 1)),
                        false,
                        new SourceFileLocation(1, 1)
                ),
                generateSimpleScope("hoge"),
                List.of(
                        new Load(6),
                        new Load(6),
                        new Push(new IntegerValue(1)),
                        new Add(),
                        new Store(6)
                )
        );
    }

    @Test
    void visitIndexExpression() {
        //まだ未実装
    }

    @Test
    void visitIntegerLiteral() {
        assertIRSimple(new IntegerLiteral(42, new SourceFileLocation(1, 1)),
                List.of(
                        new Push(new IntegerValue(42))
                ));
    }

    @Test
    void visitListLiteral() {
        //まだ未実装
    }

    @Test
    void visitStringLiteral() {
        assertIRSimple(new StringLiteral("Hoge", new SourceFileLocation(1, 1)),
                List.of(
                        new Push(new StringValue("Hoge"))
                ));
    }

    @Test
    void visitUnaryExpression() {
        assertIRSimple(
                new UnaryExpression(
                        UnaryOperator.PLUS,
                        new IntegerLiteral(2, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(new IntegerValue(2)),
                        new Plus()
                )
        );

        assertIRSimple(
                new UnaryExpression(
                        UnaryOperator.MINUS,
                        new IntegerLiteral(2, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(new IntegerValue(2)),
                        new Minus()
                )
        );

        assertIRSimple(
                new UnaryExpression(
                        UnaryOperator.NOT,
                        new BooleanLiteral(false, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(new BooleanValue(false)),
                        new Not()
                )
        );
    }

    @Test
    void visitVoidExpression() {
        assertThrows(AssertionError.class, () -> {
            assertIRSimple(
                    new VoidExpression(new SourceFileLocation(1, 1)),
                    List.of()
            );
        });
    }
}