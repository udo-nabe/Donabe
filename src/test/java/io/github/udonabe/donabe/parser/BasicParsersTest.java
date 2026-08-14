package io.github.udonabe.donabe.parser;

import io.github.udonabe.donabe.TestUtil;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.udonabe.donabe.parser.BasicParsers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BasicParsersTest {
    @Test
    void testIdentifier() {
        TokenStream success = TestUtil.toTokenStream("identifier name");
        ParseResult<Identifier> id = identifier.parse(success);
        ParseResult<Identifier> name = identifier.parse(success);
        assertEquals(new ParseSuccess<>(new Identifier("identifier", new SourceFileLocation(1, 1))), id);
        assertEquals(new ParseSuccess<>(new Identifier("name", new SourceFileLocation(1, 12))), name);
    }

    @Test
    void testExpression() {
        TokenStream success = TestUtil.toTokenStream("a * 1 + func() {return 1;} / 2 = 1");
        ParseResult<Expression> expr = expression.parse(success);
        assertEquals(new ParseSuccess<>(
                new AssignExpression(
                        new BinaryExpression(
                                new BinaryExpression(
                                        new Identifier("a", new SourceFileLocation(1, 1)),
                                        BinaryOperator.MULTIPLICATION,
                                        new IntegerLiteral(1, new SourceFileLocation(1, 5)),
                                        new SourceFileLocation(1, 1)
                                ),
                                BinaryOperator.PLUS,
                                new BinaryExpression(
                                        new FunctionLiteral(List.of(), new BlockStatement(
                                                List.of(
                                                        new ReturnStatement(
                                                                new IntegerLiteral(1, new SourceFileLocation(1, 24)),
                                                                new SourceFileLocation(1, 17)
                                                        )
                                                ),
                                                new SourceFileLocation(1, 16)
                                        ), new SourceFileLocation(1, 9)),
                                        BinaryOperator.DIVISION,
                                        new IntegerLiteral(2, new SourceFileLocation(1, 30)),
                                        new SourceFileLocation(1, 9)
                                ),
                                new SourceFileLocation(1, 1)
                        ),
                        new IntegerLiteral(1, new SourceFileLocation(1, 34)),
                        new SourceFileLocation(1, 1)
                )
        ), expr);

        TokenStream paren = TestUtil.toTokenStream("1 * (1 + 2) / 2");
        ParseResult<Expression> exprParen = expression.parse(paren);
        assertEquals(
                new ParseSuccess<>(
                        new BinaryExpression(
                                new BinaryExpression(
                                        new IntegerLiteral(1, new SourceFileLocation(1, 1)),
                                        BinaryOperator.MULTIPLICATION,
                                        new BinaryExpression(
                                                new IntegerLiteral(1, new SourceFileLocation(1, 6)),
                                                BinaryOperator.PLUS,
                                                new IntegerLiteral(2, new SourceFileLocation(1, 10)),
                                                new SourceFileLocation(1, 6)
                                        ),
                                        new SourceFileLocation(1, 1)
                                ),
                                BinaryOperator.DIVISION,
                                new IntegerLiteral(2, new SourceFileLocation(1, 15)),
                                new SourceFileLocation(1, 1)
                        )
                ),
                exprParen
        );
    }

    @Test
    void testLetDeclaration() {
        TokenStream success = TestUtil.toTokenStream("let variable = \"Hello, World\";");
        ParseResult<LetDeclaration> let = letDeclaration.parse(success);
        assertEquals(new ParseSuccess<>(
                new LetDeclaration(
                        new Identifier("variable", new SourceFileLocation(1, 5)),
                        new StringLiteral("Hello, World", new SourceFileLocation(1, 16)),
                        new SourceFileLocation(1, 1)
                )
        ), let);
    }

    @Test
    void testVarDeclaration() {
        TokenStream success = TestUtil.toTokenStream("var variable = \"Hello, World\";");
        ParseResult<VarDeclaration> let = varDeclaration.parse(success);
        assertEquals(new ParseSuccess<>(
                new VarDeclaration(
                        new Identifier("variable", new SourceFileLocation(1, 5)),
                        new StringLiteral("Hello, World", new SourceFileLocation(1, 16)),
                        new SourceFileLocation(1, 1)
                )
        ), let);
    }

    @Test
    void testExpressionStatement() {
        TokenStream success = TestUtil.toTokenStream("func(a, b) {};");
        ParseResult<ExpressionStatement> exprStatement = expressionStatement.parse(success);
        assertEquals(
                new ParseSuccess<>(
                        new ExpressionStatement(
                                new FunctionLiteral(
                                        List.of(
                                                new Identifier("a", new SourceFileLocation(1, 6)),
                                                new Identifier("b", new SourceFileLocation(1, 9))
                                        ),
                                        new BlockStatement(List.of(), new SourceFileLocation(1, 12)),
                                        new SourceFileLocation(1, 1)
                                ),
                                new SourceFileLocation(1, 1)
                        )
                ),
                exprStatement
        );
    }

    @Test
    void testWhileStatement() {
        TokenStream success = TestUtil.toTokenStream("while(true) {print(1);}");
        ParseResult<WhileStatement> whileStmt = whileStatement.parse(success);
        assertEquals(
                new ParseSuccess<>(
                        new WhileStatement(
                                new BooleanLiteral(true, new SourceFileLocation(1, 7)),
                                new BlockStatement(
                                        List.of(
                                                new ExpressionStatement(
                                                        new CallExpression(
                                                                new Identifier("print", new SourceFileLocation(1, 14)),
                                                                List.of(new IntegerLiteral(1, new SourceFileLocation(1, 20))),
                                                                new SourceFileLocation(1, 14)
                                                        ),
                                                        new SourceFileLocation(1, 14)
                                                )
                                        ),
                                        new SourceFileLocation(1, 13)
                                ),
                                new SourceFileLocation(1, 1)
                        )
                ), whileStmt
        );
    }

    @Test
    void testForStatement() {
        TokenStream success = TestUtil.toTokenStream("for var i = 0; i < 10; i++ {}");
        ParseResult<BlockStatement> forStmt = forStatement.parse(success);
        assertEquals(
                new ParseSuccess<>(
                        new BlockStatement(
                                List.of(
                                        new VarDeclaration(
                                                new Identifier("i", new SourceFileLocation(1, 9)),
                                                new IntegerLiteral(0, new SourceFileLocation(1, 13)),
                                                new SourceFileLocation(1, 5)
                                        ),
                                        new WhileStatement(
                                                new BinaryExpression(
                                                        new Identifier("i", new SourceFileLocation(1, 16)),
                                                        BinaryOperator.GREATER,
                                                        new IntegerLiteral(10, new SourceFileLocation(1, 20)),
                                                        new SourceFileLocation(1, 16)
                                                ),
                                                new BlockStatement(
                                                        List.of(
                                                                new ExpressionStatement(
                                                                        new Increment(
                                                                                new Identifier("i", new SourceFileLocation(1, 24)),
                                                                                false,
                                                                                new SourceFileLocation(1, 24)
                                                                        ), new SourceFileLocation(1, 1)
                                                                )
                                                        ), new SourceFileLocation(1, 1)
                                                ), new SourceFileLocation(1, 1)
                                        )
                                ), new SourceFileLocation(1, 1)
                        )
                ), forStmt
        );
    }
    @Test
    void testReturnStatement() {
        TokenStream success = TestUtil.toTokenStream("return 0;");
        ParseResult<ReturnStatement> ret = returnStatement.parse(success);
        assertEquals(
                new ParseSuccess<>(
                        new ReturnStatement(
                                new IntegerLiteral(0, new SourceFileLocation(1, 8)),
                                new SourceFileLocation(1, 1)
                        )
                ), ret
        );
    }

    @Test
    void testFunctionDefineStatement() {
        TokenStream success = TestUtil.toTokenStream("func test(a, b) {return a + b;}");
        ParseResult<FunctionDefineStatement> funcDef = functionDefineStatement.parse(success);
        assertEquals(new ParseSuccess<>(
                new FunctionDefineStatement(
                        new Identifier("test", new SourceFileLocation(1, 6)),
                        List.of(
                                new Identifier("a", new SourceFileLocation(1, 11)),
                                new Identifier("b", new SourceFileLocation(1, 14))
                        ),
                        new BlockStatement(
                                List.of(
                                        new ReturnStatement(
                                                new BinaryExpression(
                                                        new Identifier("a", new SourceFileLocation(1, 25)),
                                                        BinaryOperator.PLUS,
                                                        new Identifier("b", new SourceFileLocation(1, 29)),
                                                        new SourceFileLocation(1, 25)
                                                ),
                                                new SourceFileLocation(1, 18)
                                        )
                                ),
                                new SourceFileLocation(1, 17)
                        ),
                        new SourceFileLocation(1, 1)
                )
        ), funcDef);
    }

    @Test
    void testEmptyStatement() {
        TokenStream success = TestUtil.toTokenStream(";");
        ParseResult<EmptyStatement> empty = emptyStatement.parse(success);
        assertEquals(new ParseSuccess<>(new EmptyStatement(new SourceFileLocation(1, 1))), empty);
    }

    @Test
    void testForEachStatement() {
        TokenStream success = TestUtil.toTokenStream("for let i in list {}");
        ParseResult<ForEachStatement> forEach = forEachStatement.parse(success);
        assertEquals(
                new ParseSuccess<>(
                        new ForEachStatement(
                                new Identifier("i", new SourceFileLocation(1, 9)),
                                new Identifier("list", new SourceFileLocation(1, 14)),
                                new BlockStatement(List.of(), new SourceFileLocation(1, 19)),
                                new SourceFileLocation(1, 1)
                        )
                ), forEach
        );
    }

    @Test
    void testIfStatement() {
        TokenStream success = TestUtil.toTokenStream("if i {} else if true {} else {}");
        ParseResult<IfStatement> ifStmt = ifStatement().parse(success);
        assertEquals(
                new ParseSuccess<>(
                        new IfStatement(
                                new Identifier("i", new SourceFileLocation(1, 4)),
                                new BlockStatement(List.of(), new SourceFileLocation(1, 6)),
                                new IfStatement(
                                        new BooleanLiteral(true, new SourceFileLocation(1, 17)),
                                        new BlockStatement(List.of(), new SourceFileLocation(1, 22)),
                                        new BlockStatement(List.of(), new SourceFileLocation(1, 30)),
                                        new SourceFileLocation(1, 14)
                                ),
                                new SourceFileLocation(1, 1)
                        )
                ), ifStmt
        );
    }
}