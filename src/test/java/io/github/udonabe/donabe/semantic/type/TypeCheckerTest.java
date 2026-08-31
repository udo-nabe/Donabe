package io.github.udonabe.donabe.semantic.type;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ast.ASTNode;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ast.type.NamedTypeAnnotation;
import io.github.udonabe.donabe.semantic.type.builtin.*;
import io.github.udonabe.donabe.semantic.type.function.FunctionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypeCheckerTest {
    private void assertType(ASTNode target, Map<Identifier, Integer> resolution, Type type) {
        var checker = new TypeChecker("", resolution);
        assertEquals(type, target.accept(checker));
    }

    private void assertType(ASTNode target, Type type) {
        assertType(target, Map.of(), type);
    }

    private void assertTable(ASTNode target, Map<Identifier, Integer> resolution, Map<Integer, Type> expectedTable) {
        var checker = new TypeChecker("", resolution);
        target.accept(checker);
        assertEquals(expectedTable, checker.identifierTypeTable());
    }

    private void throwCompileException(ASTNode target, Map<Identifier, Integer> resolution) {
        assertThrows(CompileException.class, () -> {
            var checker = new TypeChecker("", resolution);
            target.accept(checker);
        });
    }

    private SourceFileLocation dummyLocation() {
        return new SourceFileLocation(1, 1);
    }

    @Test
    void visitProgram() {
    }

    @Test
    void visitBlockStatement() {
    }

    @Test
    void visitEmptyStatement() {
    }

    @Test
    void visitExpressionStatement() {
    }

    @Test
    void visitFunctionDefineStatement() {
        assertTable(
                new Program(List.of(
                        new FunctionDefineStatement(
                                new Identifier("test", dummyLocation()),
                                List.of(
                                        new Parameter(new Identifier("pan", dummyLocation()), new NamedTypeAnnotation(new Identifier("String", dummyLocation())))
                                ),
                                new NamedTypeAnnotation(new Identifier("Void", dummyLocation())),
                                new BlockStatement(List.of(
                                        new ReturnStatement(
                                                new VoidExpression(dummyLocation()),
                                                dummyLocation()
                                        )
                                ), dummyLocation()),
                                dummyLocation()
                        )
                ), dummyLocation()),
                Map.of(
                        new Identifier("test", dummyLocation()), 3,
                        new Identifier("pan", dummyLocation()), 4
                ),
                Map.of(
                        0, new FunctionType(List.of(new AnyType()), new VoidType()),
                        1, new FunctionType(List.of(), new StringType()),
                        2, new FunctionType(List.of(new IntType(), new IntType()), new ListType(new IntType())),
                        3, new FunctionType(List.of(new StringType()), new VoidType()),
                        4, new StringType()
                ));
    }

    @Test
    void visitIfStatement() {
        throwCompileException(new IfStatement(
                new IntegerLiteral(1, dummyLocation()),
                new BlockStatement(List.of(), dummyLocation()),
                null,
                dummyLocation()
        ), Map.of());
    }

    @Test
    void visitLetDeclaration() {
        assertTable(new LetDeclaration(
                        new Identifier("pot", dummyLocation()),
                        new StringLiteral("pot", dummyLocation()),
                        new NamedTypeAnnotation(new Identifier("String", dummyLocation())),
                        dummyLocation()
                ), Map.of(
                        new Identifier("pot", dummyLocation()), 3
                ),
                Map.of(
                        0, new FunctionType(List.of(new AnyType()), new VoidType()),
                        1, new FunctionType(List.of(), new StringType()),
                        2, new FunctionType(List.of(new IntType(), new IntType()), new ListType(new IntType())),
                        3, new StringType()
                ));
        throwCompileException(
                new LetDeclaration(
                        new Identifier("pot", dummyLocation()),
                        new IntegerLiteral(3, dummyLocation()),
                        new NamedTypeAnnotation(new Identifier("String", dummyLocation())),
                        dummyLocation()
                ), Map.of(
                        new Identifier("pot", dummyLocation()), 5
                )
        );
        throwCompileException(
                new LetDeclaration(
                        new Identifier("pot", dummyLocation()),
                        new IntegerLiteral(3, dummyLocation()),
                        new NamedTypeAnnotation(new Identifier("UnknownType", dummyLocation())),
                        dummyLocation()
                ), Map.of(
                        new Identifier("pot", dummyLocation()), 5
                )
        );
    }

    @Test
    void visitReturnStatement() {
        throwCompileException(
                new Program(List.of(
                        new FunctionDefineStatement(
                                new Identifier("test", dummyLocation()),
                                List.of(),
                                new NamedTypeAnnotation(new Identifier("Void", dummyLocation())),
                                new BlockStatement(List.of(
                                        new ReturnStatement(
                                                new StringLiteral("a", dummyLocation()),
                                                dummyLocation()
                                        )
                                ), dummyLocation()),
                                dummyLocation()
                        )
                ), dummyLocation()), Map.of(
                        new Identifier("test", dummyLocation()), 5
                ));
    }

    @Test
    void visitVarDeclaration() {
        assertTable(new VarDeclaration(
                        new Identifier("pot", dummyLocation()),
                        new StringLiteral("pot", dummyLocation()),
                        new NamedTypeAnnotation(new Identifier("String", dummyLocation())),
                        dummyLocation()
                ), Map.of(
                        new Identifier("pot", dummyLocation()), 3
                ),
                Map.of(
                        0, new FunctionType(List.of(new AnyType()), new VoidType()),
                        1, new FunctionType(List.of(), new StringType()),
                        2, new FunctionType(List.of(new IntType(), new IntType()), new ListType(new IntType())),
                        3, new StringType()
                ));
        throwCompileException(
                new VarDeclaration(
                        new Identifier("pot", dummyLocation()),
                        new IntegerLiteral(3, dummyLocation()),
                        new NamedTypeAnnotation(new Identifier("String", dummyLocation())),
                        dummyLocation()
                ), Map.of(
                        new Identifier("pot", dummyLocation()), 5
                )
        );
        throwCompileException(
                new VarDeclaration(
                        new Identifier("pot", dummyLocation()),
                        new IntegerLiteral(3, dummyLocation()),
                        new NamedTypeAnnotation(new Identifier("UnknownType", dummyLocation())),
                        dummyLocation()
                ), Map.of(
                        new Identifier("pot", dummyLocation()), 5
                )
        );
    }

    @Test
    void visitWhileStatement() {
        throwCompileException(new WhileStatement(
                new IntegerLiteral(1, dummyLocation()),
                new BlockStatement(List.of(), dummyLocation()),
                dummyLocation()
        ), Map.of());
    }

    @Test
    void visitForEachStatement() {
    }

    @Test
    void visitAssignExpression() {
        throwCompileException(
                new Program(List.of(
                        new VarDeclaration(
                                new Identifier("meat", dummyLocation()),
                                new StringLiteral("String", dummyLocation()),
                                new NamedTypeAnnotation(new Identifier("String", dummyLocation())),
                                dummyLocation()
                        ),
                        new ExpressionStatement(
                                new AssignExpression(
                                        new Identifier("meat", dummyLocation()),
                                        new IntegerLiteral(1234, dummyLocation()),
                                        dummyLocation()
                                ),
                                dummyLocation()
                        )
                ), dummyLocation()),
                Map.of(
                        new Identifier("meat", dummyLocation()), 5
                )
        );
    }

    @Test
    void visitBinaryExpression() {
        // +
        assertType(new BinaryExpression(
                new IntegerLiteral(1, dummyLocation()),
                BinaryOperator.PLUS,
                new IntegerLiteral(2, dummyLocation()),
                dummyLocation()
        ), new IntType());
        assertType(new BinaryExpression(
                new StringLiteral("Hello", dummyLocation()),
                BinaryOperator.PLUS,
                new StringLiteral(", World", dummyLocation()),
                dummyLocation()
        ), new StringType());
        throwCompileException(new BinaryExpression(
                new StringLiteral("Hello", dummyLocation()),
                BinaryOperator.PLUS,
                new IntegerLiteral(2, dummyLocation()),
                dummyLocation()
        ), Map.of());

        // -
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.MINUS,
                new IntegerLiteral(3, dummyLocation()),
                dummyLocation()
        ), new IntType());
        throwCompileException(new BinaryExpression(
                new BooleanLiteral(false, dummyLocation()),
                BinaryOperator.MINUS,
                new IntegerLiteral(2, dummyLocation()),
                dummyLocation()
        ), Map.of());

        // /
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.DIVISION,
                new IntegerLiteral(3, dummyLocation()),
                dummyLocation()
        ), new IntType());
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.DIVISION,
                new IntegerLiteral(0, dummyLocation()),
                dummyLocation()
        ), new IntType());
        throwCompileException(new BinaryExpression(
                new BooleanLiteral(false, dummyLocation()),
                BinaryOperator.DIVISION,
                new IntegerLiteral(2, dummyLocation()),
                dummyLocation()
        ), Map.of());

        // *
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.MULTIPLICATION,
                new IntegerLiteral(3, dummyLocation()),
                dummyLocation()
        ), new IntType());
        throwCompileException(new BinaryExpression(
                new VoidExpression(dummyLocation()),
                BinaryOperator.MULTIPLICATION,
                new IntegerLiteral(2, dummyLocation()),
                dummyLocation()
        ), Map.of());

        // ==
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.EQUAL,
                new IntegerLiteral(5, dummyLocation()),
                dummyLocation()
        ), new BooleanType());
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.EQUAL,
                new StringLiteral("5", dummyLocation()),
                dummyLocation()
        ), new BooleanType());

        // <
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.LESS,
                new IntegerLiteral(3, dummyLocation()),
                dummyLocation()
        ), new BooleanType());
        throwCompileException(new BinaryExpression(
                new VoidExpression(dummyLocation()),
                BinaryOperator.LESS,
                new IntegerLiteral(2, dummyLocation()),
                dummyLocation()
        ), Map.of());

        // >
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.GREATER,
                new IntegerLiteral(3, dummyLocation()),
                dummyLocation()
        ), new BooleanType());
        throwCompileException(new BinaryExpression(
                new VoidExpression(dummyLocation()),
                BinaryOperator.GREATER,
                new IntegerLiteral(2, dummyLocation()),
                dummyLocation()
        ), Map.of());

        // <=
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.LESS_EQUAL,
                new IntegerLiteral(3, dummyLocation()),
                dummyLocation()
        ), new BooleanType());
        throwCompileException(new BinaryExpression(
                new VoidExpression(dummyLocation()),
                BinaryOperator.LESS_EQUAL,
                new IntegerLiteral(2, dummyLocation()),
                dummyLocation()
        ), Map.of());

        // >=
        assertType(new BinaryExpression(
                new IntegerLiteral(5, dummyLocation()),
                BinaryOperator.GREATER_EQUAL,
                new IntegerLiteral(3, dummyLocation()),
                dummyLocation()
        ), new BooleanType());
        throwCompileException(new BinaryExpression(
                new VoidExpression(dummyLocation()),
                BinaryOperator.GREATER_EQUAL,
                new IntegerLiteral(2, dummyLocation()),
                dummyLocation()
        ), Map.of());
    }

    @Test
    void visitBooleanLiteral() {
        assertType(new BooleanLiteral(true, dummyLocation()), new BooleanType());
    }

    @Test
    void visitCallExpression() {
        throwCompileException(new Program(List.of(
                new FunctionDefineStatement(
                        new Identifier("test", dummyLocation()),
                        List.of(
                                new Parameter(new Identifier("a", dummyLocation()), new NamedTypeAnnotation(new Identifier("Int", dummyLocation())))
                        ),
                        new NamedTypeAnnotation(new Identifier("Int", dummyLocation())),
                        new BlockStatement(List.of(
                                new ReturnStatement(
                                        new Identifier("a", dummyLocation()),
                                        dummyLocation()
                                )
                        ), dummyLocation()),
                        dummyLocation()
                ),
                new ExpressionStatement(
                        new CallExpression(
                                new Identifier("test", dummyLocation()),
                                List.of(),
                                dummyLocation()
                        ), dummyLocation()
                )
        ), dummyLocation()), Map.of(
                new Identifier("test", dummyLocation()), 5,
                new Identifier("a", dummyLocation()), 6
        ));
    }

    @Test
    void visitCompoundAssignExpression() {
        throwCompileException(
                new Program(List.of(
                        new VarDeclaration(
                                new Identifier("meat", dummyLocation()),
                                new StringLiteral("String", dummyLocation()),
                                new NamedTypeAnnotation(new Identifier("String", dummyLocation())),
                                dummyLocation()
                        ),
                        new ExpressionStatement(
                                new CompoundAssignExpression(
                                        new Identifier("meat", dummyLocation()),
                                        CompoundAssignOperator.PLUS,
                                        new IntegerLiteral(1234, dummyLocation()),
                                        dummyLocation()
                                ),
                                dummyLocation()
                        )
                ), dummyLocation()),
                Map.of(
                        new Identifier("meat", dummyLocation()), 5
                )
        );
    }

    @Test
    void visitDecrement() {
        throwCompileException(
                new Program(List.of(
                        new VarDeclaration(
                                new Identifier("meat", dummyLocation()),
                                new StringLiteral("String", dummyLocation()),
                                new NamedTypeAnnotation(new Identifier("String", dummyLocation())),
                                dummyLocation()
                        ),
                        new ExpressionStatement(
                                new Decrement(
                                        new Identifier("meat", dummyLocation()),
                                        false,
                                        dummyLocation()
                                ),
                                dummyLocation()
                        )
                ), dummyLocation()),
                Map.of(
                        new Identifier("meat", dummyLocation()), 5
                )
        );
    }

    @Test
    void visitFunctionLiteral() {
        assertType(new FunctionLiteral(
                List.of(
                        new Parameter(new Identifier("meat", dummyLocation()),
                                new NamedTypeAnnotation(new Identifier("String", dummyLocation())))
                ),
                new NamedTypeAnnotation(new Identifier("String", dummyLocation())),
                new BlockStatement(
                        List.of(
                                new ReturnStatement(
                                        new Identifier("meat", dummyLocation()),
                                        dummyLocation()
                                )
                        ), dummyLocation()
                ),
                dummyLocation()
        ),                 Map.of(
                new Identifier("meat", dummyLocation()), 5
        ), new FunctionType(
                List.of(new StringType()),
                new StringType()
        ));
    }

    @Test
    void visitIdentifier() {
        assertThrows(NullPointerException.class, () -> assertType(new Identifier("notFound", dummyLocation()), new AnyType()));
    }

    @Test
    void visitIncrement() {
        throwCompileException(
                new Program(List.of(
                        new VarDeclaration(
                                new Identifier("meat", dummyLocation()),
                                new StringLiteral("String", dummyLocation()),
                                new NamedTypeAnnotation(new Identifier("String", dummyLocation())),
                                dummyLocation()
                        ),
                        new ExpressionStatement(
                                new Increment(
                                        new Identifier("meat", dummyLocation()),
                                        false,
                                        dummyLocation()
                                ),
                                dummyLocation()
                        )
                ), dummyLocation()),
                Map.of(
                        new Identifier("meat", dummyLocation()), 5
                )
        );
    }

    @Test
    void visitIndexExpression() {
    }

    @Test
    void visitIntegerLiteral() {
        assertType(new IntegerLiteral(42, dummyLocation()), new IntType());
    }

    @Test
    void visitListLiteral() {
    }

    @Test
    void visitStringLiteral() {
        assertType(new StringLiteral("Hello, World!", dummyLocation()), new StringType());
    }

    @Test
    void visitUnaryExpression() {
        //+
        assertType(
                new UnaryExpression(UnaryOperator.PLUS, new IntegerLiteral(42, dummyLocation()), dummyLocation()),
                new IntType()
        );
        //-
        assertType(
                new UnaryExpression(UnaryOperator.MINUS, new IntegerLiteral(42, dummyLocation()), dummyLocation()),
                new IntType()
        );
        //!
        assertType(
                new UnaryExpression(UnaryOperator.NOT, new BooleanLiteral(false, dummyLocation()), dummyLocation()),
                new BooleanType()
        );

        throwCompileException(
                new UnaryExpression(UnaryOperator.NOT, new StringLiteral("false", dummyLocation()), dummyLocation()), Map.of()
        );
        throwCompileException(
                new UnaryExpression(UnaryOperator.PLUS, new BooleanLiteral(false, dummyLocation()), dummyLocation()), Map.of()
        );
        throwCompileException(
                new UnaryExpression(UnaryOperator.MINUS, new StringLiteral("false", dummyLocation()), dummyLocation()), Map.of()
        );
    }

    @Test
    void visitVoidExpression() {
        assertType(new VoidExpression(dummyLocation()), new VoidType());
    }

    @Test
    void visitNamedTypeAnnotation() {
    }

    @Test
    void visitFunctionTypeAnnotation() {
    }

    @Test
    void visitParameter() {
    }
}