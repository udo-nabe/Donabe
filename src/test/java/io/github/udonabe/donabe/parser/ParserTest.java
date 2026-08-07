package io.github.udonabe.donabe.parser;

import io.github.udonabe.donabe.TestUtil;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.lexer.Token;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {
    private static TokenStream streamForTest;
    @BeforeAll
    static void setUp() {
        streamForTest = new TokenStream(List.of());
    }
    @Test
    void testMap() {
        //マップ元が成功した場合
        Parser<String> stringParser = stream -> new ParseSuccess<>("1234");
        Parser<Integer> intParser = stringParser.map(Integer::parseInt);
        assertEquals(new ParseSuccess<>(1234), intParser.parse(streamForTest));
        //マップ元が失敗した場合
        Parser<String> failParser = stream -> new ParseFailed<>("Always fail.", 0);
        Parser<Integer> parser = failParser.map(Integer::parseInt);
        assertEquals(new ParseFailed<>("Always fail.", 0), parser.parse(streamForTest));
        //マップ中に例外を捕捉した場合
        stringParser = stream -> new ParseSuccess<>("NaN");
        intParser = stringParser.map(Integer::parseInt);
        assertEquals(ParseFailed.class, intParser.parse(streamForTest).getClass());
    }

    @Test
    void then() {
        //両方成功した場合
        Parser<String> firstParser = stream -> new ParseSuccess<>("Hello");
        Parser<String> lastParser = stream -> new ParseSuccess<>("World");
        assertEquals(new ParseSuccess<>(Pair.of("Hello", "World")), firstParser.then(lastParser).parse(streamForTest));
        //一つ目が失敗した場合
        firstParser = stream -> new ParseFailed<>("Test", 0);
        assertEquals(new ParseFailed<>("Test", 0), firstParser.then(lastParser).parse(streamForTest));
        //二つ目が失敗した場合
        firstParser = stream -> new ParseSuccess<>("Hello");
        lastParser = stream -> new ParseFailed<>("Test", 0);
        assertEquals(new ParseFailed<>("Test", 0), firstParser.then(lastParser).parse(streamForTest));
        //両方失敗した場合、一つ目の失敗が返されるか
        firstParser = stream -> new ParseFailed<>("First", 0);
        assertEquals(new ParseFailed<>("First", 0), firstParser.then(lastParser).parse(streamForTest));
        //例外が発生した場合、伝搬するか
        Parser<String> firstThrow1 = stream -> { throw new RuntimeException("Test"); };
        Parser<String> lastThrow1 = stream -> new ParseSuccess<>("Success");
        assertThrows(RuntimeException.class, () -> firstThrow1.then(lastThrow1).parse(streamForTest));

        Parser<String> firstThrow2 = stream -> new ParseSuccess<>("Success");
        Parser<String> lastThrow2 = stream -> { throw new RuntimeException("Test"); };
        assertThrows(RuntimeException.class, () -> firstThrow2.then(lastThrow2).parse(streamForTest));
    }

    @Test
    void to() {
        //両方成功した場合
        Parser<String> firstParser = stream -> new ParseSuccess<>("Hello");
        Parser<String> lastParser = stream -> new ParseSuccess<>("World");
        assertEquals(new ParseSuccess<>("World"), firstParser.to(lastParser).parse(streamForTest));
        //一つ目が失敗した場合
        firstParser = stream -> new ParseFailed<>("Test", 0);
        assertEquals(new ParseFailed<>("Test", 0), firstParser.to(lastParser).parse(streamForTest));
        //二つ目が失敗した場合
        firstParser = stream -> new ParseSuccess<>("Hello");
        lastParser = stream -> new ParseFailed<>("Test", 0);
        assertEquals(new ParseFailed<>("Test", 0), firstParser.to(lastParser).parse(streamForTest));
        //両方失敗した場合、一つ目の失敗が返されるか
        firstParser = stream -> new ParseFailed<>("First", 0);
        assertEquals(new ParseFailed<>("First", 0), firstParser.to(lastParser).parse(streamForTest));
        //例外が発生した場合、伝搬するか
        Parser<String> firstThrow1 = stream -> { throw new RuntimeException("Test"); };
        Parser<String> lastThrow1 = stream -> new ParseSuccess<>("Success");
        assertThrows(RuntimeException.class, () -> firstThrow1.to(lastThrow1).parse(streamForTest));

        Parser<String> firstThrow2 = stream -> new ParseSuccess<>("Success");
        Parser<String> lastThrow2 = stream -> { throw new RuntimeException("Test"); };
        assertThrows(RuntimeException.class, () -> firstThrow2.to(lastThrow2).parse(streamForTest));
    }

    @Test
    void skip() {
        //両方成功した場合
        Parser<String> firstParser = stream -> new ParseSuccess<>("Hello");
        Parser<String> lastParser = stream -> new ParseSuccess<>("World");
        assertEquals(new ParseSuccess<>("Hello"), firstParser.skip(lastParser).parse(streamForTest));
        //一つ目が失敗した場合
        firstParser = stream -> new ParseFailed<>("Test", 0);
        assertEquals(new ParseFailed<>("Test", 0), firstParser.skip(lastParser).parse(streamForTest));
        //二つ目が失敗した場合
        firstParser = stream -> new ParseSuccess<>("Hello");
        lastParser = stream -> new ParseFailed<>("Test", 0);
        assertEquals(new ParseFailed<>("Test", 0), firstParser.skip(lastParser).parse(streamForTest));
        //両方失敗した場合、一つ目の失敗が返されるか
        firstParser = stream -> new ParseFailed<>("First", 0);
        assertEquals(new ParseFailed<>("First", 0), firstParser.skip(lastParser).parse(streamForTest));
        //例外が発生した場合、伝搬するか
        Parser<String> firstThrow1 = stream -> { throw new RuntimeException("Test"); };
        Parser<String> lastThrow1 = stream -> new ParseSuccess<>("Success");
        assertThrows(RuntimeException.class, () -> firstThrow1.skip(lastThrow1).parse(streamForTest));

        Parser<String> firstThrow2 = stream -> new ParseSuccess<>("Success");
        Parser<String> lastThrow2 = stream -> { throw new RuntimeException("Test"); };
        assertThrows(RuntimeException.class, () -> firstThrow2.skip(lastThrow2).parse(streamForTest));
    }

    @Test
    void between() {
        TokenStream st = TestUtil.toTokenStream("(let)");
        //正しく挟まれている場合
        Parser<Token> body = Parsers.token(Token.Kind.LET);
        Parser<Token> left = Parsers.token(Token.Kind.LPAREN);
        Parser<Token> right = Parsers.token(Token.Kind.RPAREN);
        assertEquals(new ParseSuccess<>(new Token(Token.Kind.LET, "let", "(let)", 1, 2)), body.between(left, right).parse(st));
        assertEquals(3, st.pos());
        //順序が逆の場合
        TokenStream reversed = TestUtil.toTokenStream(")let(");
        assertEquals(ParseFailed.class, body.between(left, right).parse(reversed).getClass());
        assertEquals(0, reversed.pos());
        //閉じられていない場合
        TokenStream unclosed  = TestUtil.toTokenStream("(let");
        assertEquals(ParseFailed.class, body.between(left, right).parse(unclosed).getClass());
        assertEquals(2, unclosed.pos());
        //開始が無い場合
        TokenStream unopened = TestUtil.toTokenStream("let)");
        assertEquals(ParseFailed.class, body.between(left, right).parse(unopened).getClass());
        assertEquals(0, unopened.pos());
        //例外が起きた場合伝搬するか
        Parser<Token> bodyThrow = stream -> {throw new RuntimeException("Test");};
        Parser<Token> leftThrow = stream -> {throw new RuntimeException("Test");};
        Parser<Token> rightThrow = stream -> {throw new RuntimeException("Test");};

        TokenStream throwStream1 = TestUtil.toTokenStream("(let)");
        assertThrows(RuntimeException.class, () -> bodyThrow.between(left, right).parse(throwStream1));
        TokenStream throwStream2 = TestUtil.toTokenStream("(let)");
        assertThrows(RuntimeException.class, () -> body.between(leftThrow, right).parse(throwStream2));
        TokenStream throwStream3 = TestUtil.toTokenStream("(let)");
        assertThrows(RuntimeException.class, () -> body.between(left, rightThrow).parse(throwStream3));
    }

    @Test
    void optional() {
        //成功した場合、正しくOptionalにラップされるか
        TokenStream success = TestUtil.toTokenStream("let");
        Parser<Optional<Token>> optional = Parsers.token(Token.Kind.LET).optional();
        assertEquals(new ParseSuccess<>(Optional.of(new Token(Token.Kind.LET, "let", "let", 1, 1))), optional.parse(success));
        assertEquals(1, success.pos());
        //失敗した場合、Optional.empty()が返されるか
        TokenStream fail = TestUtil.toTokenStream("fail");
        assertEquals(new ParseSuccess<>(Optional.empty()), optional.parse(fail));
        assertEquals(0, fail.pos());
        //例外が発生した場合、伝搬するか
        TokenStream failStream = TestUtil.toTokenStream("let");
        Parser<Object> alwaysFail = stream -> {throw new RuntimeException("Test");};
        assertThrows(RuntimeException.class, () -> alwaysFail.parse(failStream));
    }
}
