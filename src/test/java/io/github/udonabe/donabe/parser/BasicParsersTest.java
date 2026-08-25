package io.github.udonabe.donabe.parser;

import io.github.udonabe.donabe.TestUtil;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ast.type.FunctionTypeAnnotation;
import io.github.udonabe.donabe.ast.type.NamedTypeAnnotation;
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
        TokenStream success = TestUtil.toTokenStream("a * 1 + func(): int {return 1;} / 2 = 1");
        ParseResult<Expression> expr = expression.parse(success);
        assertEquals("ParseSuccess[value=AssignExpression[target=BinaryExpression[left=BinaryExpression[left=Identifier[name=a, location=SourceFileLocation[line=1, column=1]], operator=MULTIPLICATION, right=IntegerLiteral[value=1, location=SourceFileLocation[line=1, column=5]], location=SourceFileLocation[line=1, column=1]], operator=PLUS, right=BinaryExpression[left=FunctionLiteral[args=[], type=NamedTypeAnnotation[name=Identifier[name=int, location=SourceFileLocation[line=1, column=17]]], block=BlockStatement[statements=[ReturnStatement[returnValue=IntegerLiteral[value=1, location=SourceFileLocation[line=1, column=29]], location=SourceFileLocation[line=1, column=22]]], location=SourceFileLocation[line=1, column=21]], location=SourceFileLocation[line=1, column=9]], operator=DIVISION, right=IntegerLiteral[value=2, location=SourceFileLocation[line=1, column=35]], location=SourceFileLocation[line=1, column=9]], location=SourceFileLocation[line=1, column=1]], value=IntegerLiteral[value=1, location=SourceFileLocation[line=1, column=39]], location=SourceFileLocation[line=1, column=1]]]",
                expr.toString());

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
    void testCompare() {
        TokenStream lessStream = TestUtil.toTokenStream("1 < 2");
        ParseResult<Expression> less = expression.parse(lessStream);
        assertEquals(
                new ParseSuccess<>(
                        new BinaryExpression(
                                new IntegerLiteral(1, new SourceFileLocation(1, 1)),
                                BinaryOperator.LESS,
                                new IntegerLiteral(2, new SourceFileLocation(1, 5)),
                                new SourceFileLocation(1, 1)
                        )
                ), less
        );

        TokenStream greaterStream = TestUtil.toTokenStream("1 > 2");
        ParseResult<Expression> greater = expression.parse(greaterStream);
        assertEquals(
                new ParseSuccess<>(
                        new BinaryExpression(
                                new IntegerLiteral(1, new SourceFileLocation(1, 1)),
                                BinaryOperator.GREATER,
                                new IntegerLiteral(2, new SourceFileLocation(1, 5)),
                                new SourceFileLocation(1, 1)
                        )
                ), greater
        );

        TokenStream lessEqualStream = TestUtil.toTokenStream("1 <= 2");
        ParseResult<Expression> lessEqual = expression.parse(lessEqualStream);
        assertEquals(
                new ParseSuccess<>(
                        new BinaryExpression(
                                new IntegerLiteral(1, new SourceFileLocation(1, 1)),
                                BinaryOperator.LESS_EQUAL,
                                new IntegerLiteral(2, new SourceFileLocation(1, 6)),
                                new SourceFileLocation(1, 1)
                        )
                ), lessEqual
        );

        TokenStream greaterEqualStream = TestUtil.toTokenStream("1 >= 2");
        ParseResult<Expression> greaterEqual = expression.parse(greaterEqualStream);
        assertEquals(
                new ParseSuccess<>(
                        new BinaryExpression(
                                new IntegerLiteral(1, new SourceFileLocation(1, 1)),
                                BinaryOperator.GREATER_EQUAL,
                                new IntegerLiteral(2, new SourceFileLocation(1, 6)),
                                new SourceFileLocation(1, 1)
                        )
                ), greaterEqual
        );
    }

    @Test
    void testLetDeclaration() {
        TokenStream success = TestUtil.toTokenStream("let variable: string = \"Hello, World\";");
        ParseResult<LetDeclaration> let = letDeclaration.parse(success);
        assertEquals(new ParseSuccess<>(
                new LetDeclaration(
                        new Identifier("variable", new SourceFileLocation(1, 5)),
                        new StringLiteral("Hello, World", new SourceFileLocation(1, 24)),
                        new NamedTypeAnnotation(new Identifier("string", new SourceFileLocation(1, 15))),
                        new SourceFileLocation(1, 1)
                )
        ), let);
    }

    @Test
    void testVarDeclaration() {
        TokenStream success = TestUtil.toTokenStream("var variable: string = \"Hello, World\";");
        ParseResult<VarDeclaration> let = varDeclaration.parse(success);
        assertEquals(new ParseSuccess<>(
                new VarDeclaration(
                        new Identifier("variable", new SourceFileLocation(1, 5)),
                        new StringLiteral("Hello, World", new SourceFileLocation(1, 24)),
                        new NamedTypeAnnotation(new Identifier("string", new SourceFileLocation(1, 15))),
                        new SourceFileLocation(1, 1)
                )
        ), let);
    }

    @Test
    void testExpressionStatement() {
        TokenStream success = TestUtil.toTokenStream("func(a: int, b: int): void {};");
        ParseResult<ExpressionStatement> exprStatement = expressionStatement.parse(success);
        assertEquals("ParseSuccess[value=ExpressionStatement[expression=FunctionLiteral[args=[Parameter[name=Identifier[name=a, location=SourceFileLocation[line=1, column=6]], type=NamedTypeAnnotation[name=Identifier[name=int, location=SourceFileLocation[line=1, column=9]]]], Parameter[name=Identifier[name=b, location=SourceFileLocation[line=1, column=14]], type=NamedTypeAnnotation[name=Identifier[name=int, location=SourceFileLocation[line=1, column=17]]]]], type=NamedTypeAnnotation[name=Identifier[name=void, location=SourceFileLocation[line=1, column=23]]], block=BlockStatement[statements=[], location=SourceFileLocation[line=1, column=28]], location=SourceFileLocation[line=1, column=1]], location=SourceFileLocation[line=1, column=1]]]",
                exprStatement.toString()
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
        TokenStream success = TestUtil.toTokenStream("for var i: int = 0; i < 10; i++ {}");
        ParseResult<BlockStatement> forStmt = forStatement.parse(success);
        assertEquals(
                "ParseSuccess[value=BlockStatement[statements=[VarDeclaration[name=Identifier[name=i, location=SourceFileLocation[line=1, column=9]], expr=IntegerLiteral[value=0, location=SourceFileLocation[line=1, column=18]], type=NamedTypeAnnotation[name=Identifier[name=int, location=SourceFileLocation[line=1, column=12]]], location=SourceFileLocation[line=1, column=5]], WhileStatement[condition=BinaryExpression[left=Identifier[name=i, location=SourceFileLocation[line=1, column=21]], operator=LESS, right=IntegerLiteral[value=10, location=SourceFileLocation[line=1, column=25]], location=SourceFileLocation[line=1, column=21]], loop=BlockStatement[statements=[ExpressionStatement[expression=Increment[target=Identifier[name=i, location=SourceFileLocation[line=1, column=29]], prefix=false, location=SourceFileLocation[line=1, column=29]], location=SourceFileLocation[line=1, column=1]]], location=SourceFileLocation[line=1, column=1]], location=SourceFileLocation[line=1, column=1]]], location=SourceFileLocation[line=1, column=1]]]",
                forStmt.toString());
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
        TokenStream success = TestUtil.toTokenStream("func test(a: int, b: int)->int {return a + b;}");
        ParseResult<FunctionDefineStatement> funcDef = functionDefineStatement.parse(success);
        assertEquals("ParseSuccess[value=FunctionDefineStatement[identifier=Identifier[name=test, location=SourceFileLocation[line=1, column=6]], args=[Parameter[name=Identifier[name=a, location=SourceFileLocation[line=1, column=11]], type=NamedTypeAnnotation[name=Identifier[name=int, location=SourceFileLocation[line=1, column=14]]]], Parameter[name=Identifier[name=b, location=SourceFileLocation[line=1, column=19]], type=NamedTypeAnnotation[name=Identifier[name=int, location=SourceFileLocation[line=1, column=22]]]]], returnValue=NamedTypeAnnotation[name=Identifier[name=int, location=SourceFileLocation[line=1, column=28]]], block=BlockStatement[statements=[ReturnStatement[returnValue=BinaryExpression[left=Identifier[name=a, location=SourceFileLocation[line=1, column=40]], operator=PLUS, right=Identifier[name=b, location=SourceFileLocation[line=1, column=44]], location=SourceFileLocation[line=1, column=40]], location=SourceFileLocation[line=1, column=33]]], location=SourceFileLocation[line=1, column=32]], location=SourceFileLocation[line=1, column=1]]]",
                funcDef.toString());
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

    @Test
    void testNamedTypeAnnotation() {
        TokenStream success = TestUtil.toTokenStream("int");
        ParseResult<NamedTypeAnnotation> type = namedType.parse(success);
        assertEquals(
                new ParseSuccess<>(
                        new NamedTypeAnnotation(
                                new Identifier("int", new SourceFileLocation(1, 1))
                        )
                ), type
        );
    }

    @Test
    void testFunctionTypeAnnotation() {
        TokenStream success = TestUtil.toTokenStream("(int, string) -> void");
        ParseResult<FunctionTypeAnnotation> type = functionType.parse(success);
        assertEquals(
                new ParseSuccess<>(
                        new FunctionTypeAnnotation(
                                List.of(
                                        new NamedTypeAnnotation(
                                                new Identifier("int", new SourceFileLocation(1, 2))
                                        ),
                                        new NamedTypeAnnotation(
                                                new Identifier("string", new SourceFileLocation(1, 7))
                                        )
                                ),
                                new NamedTypeAnnotation(
                                        new Identifier("void", new SourceFileLocation(1, 18))
                                ),
                                new SourceFileLocation(1, 1)
                        )
                ), type
        );

        TokenStream nest = TestUtil.toTokenStream("(int, string) -> () -> int");
        ParseResult<FunctionTypeAnnotation> type1 = functionType.parse(nest);
        assertEquals(
                new ParseSuccess<>(
                        new FunctionTypeAnnotation(
                                List.of(
                                        new NamedTypeAnnotation(
                                                new Identifier("int", new SourceFileLocation(1, 2))
                                        ),
                                        new NamedTypeAnnotation(
                                                new Identifier("string", new SourceFileLocation(1, 7))
                                        )
                                ),
                                new FunctionTypeAnnotation(
                                        List.of(),
                                        new NamedTypeAnnotation(
                                                new Identifier("int", new SourceFileLocation(1, 24))
                                        ),
                                        new SourceFileLocation(1, 18)
                                ),
                                new SourceFileLocation(1, 1)
                        )
                ), type1
        );
    }
}