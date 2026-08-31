package io.github.udonabe.donabe.semantic.ir;

import io.github.udonabe.donabe.ast.ASTNode;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ast.type.NamedTypeAnnotation;
import io.github.udonabe.donabe.ir.IRLocation;
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
    private void assertIRSimple(ASTNode ast, List<Instruction> instructions) {
        assertIR(ast, Map.of(), instructions);
    }

    private void assertIR(ASTNode ast, Map<Identifier, Integer> resolution, List<Instruction> instructions) {
        assertIR(ast, instructions, resolution, new HashMap<>());
    }

    private void assertIR(ASTNode ast, List<Instruction> instructions, Map<Identifier, Integer> resolution, Map<ASTNode, Set<Integer>> map) {
        List<Instruction> actualInstructions = ast.accept(new IRGenerator(resolution, Set.copyOf(resolution.values()), map));
        assertIterableEquals(instructions, actualInstructions);
    }

    private IRLocation dummyLocation() {
        return new IRLocation(1);
    }

    @Test
    void visitProgram() {
        assertIR(
                new Program(
                        List.of(
                                new LetDeclaration(
                                        new Identifier("foo", new SourceFileLocation(1, 1)),
                                        new IntegerLiteral(42, new SourceFileLocation(1, 1)),
                                        new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1))),
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
                                        new NamedTypeAnnotation(new Identifier("string", new SourceFileLocation(1, 1))),
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
                Map.of(new Identifier("foo", new SourceFileLocation(1, 1)), 6,
                        new Identifier("bar", new SourceFileLocation(1, 1)),7),
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new StoreLocal(6, dummyLocation()),
                        new LoadLocal(6, dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Add(dummyLocation()),
                        new StoreLocal(7, dummyLocation()),
                        new LoadLocal(6, dummyLocation()),
                        new LoadLocal(7, dummyLocation()),
                        new Sub(dummyLocation()),
                        new Pop(dummyLocation())
                )
        );
    }

    @Test
    void visitBlockStatement() {
        assertIR(
                new BlockStatement(
                        List.of(
                                new LetDeclaration(
                                        new Identifier("foo", new SourceFileLocation(1, 1)),
                                        new IntegerLiteral(42, new SourceFileLocation(1, 1)),
                                        new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1))),
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
                                        new NamedTypeAnnotation(new Identifier("string", new SourceFileLocation(1, 1))),
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
                Map.of(new Identifier("foo", new SourceFileLocation(1, 1)), 6,
                        new Identifier("bar", new SourceFileLocation(1, 1)),7),
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new StoreLocal(6, dummyLocation()),
                        new LoadLocal(6, dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Add(dummyLocation()),
                        new StoreLocal(7, dummyLocation()),
                        new LoadLocal(6, dummyLocation()),
                        new LoadLocal(7, dummyLocation()),
                        new Sub(dummyLocation()),
                        new Pop(dummyLocation())
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
                        new Push(new IntegerValue(123), dummyLocation()),
                        new Pop(dummyLocation())
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
                Set.of(8, 9),
                List.of(
                        new LoadCaptured(7, dummyLocation()),
                        new Pop(dummyLocation()),
                        new LoadLocal(8, dummyLocation()),
                        new LoadLocal(9, dummyLocation()),
                        new Add(dummyLocation()),
                        new Return(dummyLocation())
                )
        );

        var define = new FunctionDefineStatement(
                new Identifier("add", new SourceFileLocation(1, 1)),
                List.of(
                        new Parameter(
                                new Identifier("a", new SourceFileLocation(1, 1)),
                                new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1)))
                        ),
                        new Parameter(
                                new Identifier("b", new SourceFileLocation(1, 1)),
                                new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1)))
                        )
                ),
                new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1))),
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
                                        new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1))),
                                        new SourceFileLocation(1, 1)
                                ),
                                define
                        ),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(functionValue, dummyLocation()),
                        new StoreLocal(6, dummyLocation()),
                        new Push(new IntegerValue(42), dummyLocation()),
                        new StoreLocal(7, dummyLocation())
                ),
                Map.of(new Identifier("add", new SourceFileLocation(1, 1)), 6,
                        new Identifier("answer", new SourceFileLocation(1, 1)),7,
                        new Identifier("a", new SourceFileLocation(1, 1)), 8,
                        new Identifier("b", new SourceFileLocation(1, 1)),9),
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
                Map.of(),
                List.of(
                        new Push(new BooleanValue(true), dummyLocation()),
                        new JmpFalse(new Label(".0"), dummyLocation()),

                        new Push(new IntegerValue(1), dummyLocation()),
                        new Pop(dummyLocation()),
                        new Jmp(new Label(".1"), dummyLocation()),

                        new LabelNop(new Label(".0"), dummyLocation()),
                        new Push(new IntegerValue(3), dummyLocation()),
                        new Pop(dummyLocation()),
                        new Jmp(new Label(".1"), dummyLocation()),

                        new LabelNop(new Label(".1"), dummyLocation())
                )
        );

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
                Map.of(),
                List.of(
                        new Push(new BooleanValue(true), dummyLocation()),
                        new JmpFalse(new Label(".0"), dummyLocation()),

                        new Push(new IntegerValue(1), dummyLocation()),
                        new Pop(dummyLocation()),

                        new LabelNop(new Label(".0"), dummyLocation())
                )
        );
    }

    @Test
    void visitLetDeclaration() {
        assertIR(
                new LetDeclaration(
                        new Identifier("bar", new SourceFileLocation(1, 1)),
                        new StringLiteral("Hello", new SourceFileLocation(1, 1)),
                        new NamedTypeAnnotation(new Identifier("string", new SourceFileLocation(1, 1))),
                        new SourceFileLocation(1, 1)
                ),
                Map.of(new Identifier("bar", new SourceFileLocation(1, 1)), 6),
                List.of(
                        new Push(new StringValue("Hello"), dummyLocation()),
                        new StoreLocal(6, dummyLocation())
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
                        new Push(new IntegerValue(1234), dummyLocation()),
                        new Return(dummyLocation())
                )
        );

        //値が無い場合(VoidExpression)
        assertIRSimple(
                new ReturnStatement(
                        new VoidExpression(new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new VoidReturn(dummyLocation())
                )
        );
    }

    @Test
    void visitVarDeclaration() {
        assertIR(
                new VarDeclaration(
                        new Identifier("hoge", new SourceFileLocation(1, 1)),
                        new StringLiteral("Hello", new SourceFileLocation(1, 1)),
                        new NamedTypeAnnotation(new Identifier("string", new SourceFileLocation(1, 1))),
                        new SourceFileLocation(1, 1)
                ),
                Map.of(new Identifier("hoge", new SourceFileLocation(1, 1)), 6),
                List.of(
                        new Push(new StringValue("Hello"), dummyLocation()),
                        new StoreLocal(6, dummyLocation())
                )
        );
    }

    @Test
    void visitWhileStatement() {
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
                Map.of(),
                List.of(
                        new LabelNop(new Label(".0"), dummyLocation()),

                        new Push(new BooleanValue(true), dummyLocation()),
                        new JmpFalse(new Label(".1"), dummyLocation()),

                        new Push(new IntegerValue(42), dummyLocation()),
                        new Pop(dummyLocation()),

                        new Jmp(new Label(".0"), dummyLocation()),

                        new LabelNop(new Label(".1"), dummyLocation())
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
                Map.of(new Identifier("bar", new SourceFileLocation(1, 1)), 6),
                List.of(
                        new Push(new StringValue("Hello"), dummyLocation()),
                        new StoreLocal(6, dummyLocation()),
                        new LoadLocal(6, dummyLocation())
                )
        );
    }

    private void binaryExpression(BinaryOperator operator) {
        Instruction operatorInstruction = switch (operator) {
            case PLUS -> new Add(dummyLocation());
            case MINUS -> new Sub(dummyLocation());
            case MULTIPLICATION -> new Mul(dummyLocation());
            case DIVISION -> new Div(dummyLocation());
            case EQUAL -> new Equal(dummyLocation());
            case LESS -> new Less(dummyLocation());
            case GREATER -> new Greater(dummyLocation());
            case LESS_EQUAL -> new LessEqual(dummyLocation());
            case GREATER_EQUAL -> new GreaterEqual(dummyLocation());
        };

        assertIRSimple(
                new BinaryExpression(
                        new IntegerLiteral(42, new SourceFileLocation(1, 1)),
                        operator,
                        new IntegerLiteral(3, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(new IntegerValue(42), dummyLocation()),
                        new Push(new IntegerValue(3), dummyLocation()),
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
                        new Push(new BooleanValue(true), dummyLocation())
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
                Set.of(7, 8),
                List.of(
                        new LoadLocal(7, dummyLocation()),
                        new LoadLocal(8, dummyLocation()),
                        new Add(dummyLocation()),
                        new Return(dummyLocation())
                )
        );

        var define = new FunctionDefineStatement(
                new Identifier("add", new SourceFileLocation(1, 1)),
                List.of(
                        new Parameter(
                                new Identifier("a", new SourceFileLocation(1, 1)),
                                new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1)))
                        ),
                        new Parameter(
                                new Identifier("b", new SourceFileLocation(1, 1)),
                                new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1)))
                        )
                ),
                new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1))),
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
                List.of(
                        new Push(functionValue, dummyLocation()),
                        new StoreLocal(6, dummyLocation()),
                        new Push(new IntegerValue(1234), dummyLocation()),
                        new Push(new IntegerValue(123), dummyLocation()),
                        new LoadLocal(6, dummyLocation()),
                        new Call(dummyLocation()),
                        new Pop(dummyLocation())
                ),
                Map.of(new Identifier("add", new SourceFileLocation(1, 1)), 6,
                        new Identifier("a", new SourceFileLocation(1, 1)),7,
                        new Identifier("b", new SourceFileLocation(1, 1)), 8),
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
                Map.of(new Identifier("bar", new SourceFileLocation(1, 1)), 6),
                List.of(
                        new LoadLocal(6, dummyLocation()),
                        new Push(new IntegerValue(1234), dummyLocation()),
                        new Add(dummyLocation()),
                        new StoreLocal(6, dummyLocation()),
                        new LoadLocal(6, dummyLocation())
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
                Map.of(new Identifier("hoge", new SourceFileLocation(1, 1)), 6),
                List.of(
                        new LoadLocal(6, dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Sub(dummyLocation()),
                        new StoreLocal(6, dummyLocation()),
                        new LoadLocal(6, dummyLocation())
                )
        );

        //後置
        assertIR(
                new Decrement(
                        new Identifier("hoge", new SourceFileLocation(1, 1)),
                        false,
                        new SourceFileLocation(1, 1)
                ),
                Map.of(new Identifier("hoge", new SourceFileLocation(1, 1)), 6),
                List.of(
                        new LoadLocal(6, dummyLocation()),
                        new LoadLocal(6, dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Sub(dummyLocation()),
                        new StoreLocal(6, dummyLocation())
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
                Set.of(6, 7),
                List.of(
                        new LoadLocal(6, dummyLocation()),
                        new LoadLocal(7, dummyLocation()),
                        new Add(dummyLocation()),
                        new Return(dummyLocation())
                )
        );

        var define = new FunctionLiteral(
                List.of(
                        new Parameter(
                                new Identifier("a", new SourceFileLocation(1, 1)),
                                new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1)))
                        ),
                        new Parameter(
                                new Identifier("b", new SourceFileLocation(1, 1)),
                                new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1)))
                        )
                ),
                new NamedTypeAnnotation(new Identifier("int", new SourceFileLocation(1, 1))),
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
                List.of(
                        new Push(functionValue, dummyLocation())
                ),
                Map.of(new Identifier("a", new SourceFileLocation(1, 1)), 6,
                        new Identifier("b", new SourceFileLocation(1, 1)), 7),
                Map.of(
                        define, Set.of(6, 7)
                )
        );
    }

    @Test
    void visitIdentifier() {
        assertIR(
                new Identifier("foo", new SourceFileLocation(1, 1)),
                Map.of(new Identifier("foo", new SourceFileLocation(1, 1)), 6),
                List.of(
                        new LoadLocal(6, dummyLocation())
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
                Map.of(new Identifier("hoge", new SourceFileLocation(1, 1)), 6),
                List.of(
                        new LoadLocal(6, dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Add(dummyLocation()),
                        new StoreLocal(6, dummyLocation()),
                        new LoadLocal(6, dummyLocation())
                )
        );

        //後置
        assertIR(
                new Increment(
                        new Identifier("hoge", new SourceFileLocation(1, 1)),
                        false,
                        new SourceFileLocation(1, 1)
                ),
                Map.of(new Identifier("hoge", new SourceFileLocation(1, 1)), 6),
                List.of(
                        new LoadLocal(6, dummyLocation()),
                        new LoadLocal(6, dummyLocation()),
                        new Push(new IntegerValue(1), dummyLocation()),
                        new Add(dummyLocation()),
                        new StoreLocal(6, dummyLocation())
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
                        new Push(new IntegerValue(42), dummyLocation())
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
                        new Push(new StringValue("Hoge"), dummyLocation())
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
                        new Push(new IntegerValue(2), dummyLocation()),
                        new Plus(dummyLocation())
                )
        );

        assertIRSimple(
                new UnaryExpression(
                        UnaryOperator.MINUS,
                        new IntegerLiteral(2, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(new IntegerValue(2), dummyLocation()),
                        new Minus(dummyLocation())
                )
        );

        assertIRSimple(
                new UnaryExpression(
                        UnaryOperator.NOT,
                        new BooleanLiteral(false, new SourceFileLocation(1, 1)),
                        new SourceFileLocation(1, 1)
                ),
                List.of(
                        new Push(new BooleanValue(false), dummyLocation()),
                        new Not(dummyLocation())
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