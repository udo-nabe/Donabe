package io.github.udonabe.donabe.parser;

import io.github.udonabe.donabe.TestUtil;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.lexer.Token;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.udonabe.donabe.parser.Parsers.*;
import static org.junit.jupiter.api.Assertions.*;

class ParsersTest {
    @Test
    void testToken() {
        //トークンが一致する場合、進めてそのトークンを返すか
        TokenStream success = TestUtil.toTokenStream("if else");
        assertEquals(new ParseSuccess<>(new Token(Token.Kind.IF, "if", "if else", 1, 1)),
                token(Token.Kind.IF).parse(success));
        assertEquals(1, success.pos());
        assertEquals(new ParseSuccess<>(new Token(Token.Kind.ELSE, "else", "if else", 1, 4)),
                token(Token.Kind.ELSE).parse(success));
        assertEquals(2, success.pos());
        //トークンが一致しない場合、進めずにParseFailedを返すか
        TokenStream unmatch = TestUtil.toTokenStream("while");
        assertEquals(new ParseFailed<>("""
                        [line 1, column 1] Unexpected token: 'while'. Expected: 'FOR'
                        while
                        ^
                        """, 0),
                token(Token.Kind.FOR).parse(unmatch));
        assertEquals(0, unmatch.pos());
        //トークンが存在しない場合、ParseFailedを返すか
        TokenStream unexist = TestUtil.toTokenStream("");
        assertEquals(new ParseFailed<>("""
                        [line 1, column 1] Unexpected token: '<EOF>'. Expected: 'FOR'
                        
                        ^
                        """, 0),
                token(Token.Kind.FOR).parse(unexist));
        assertEquals(0, unexist.pos());
    }

    @Test
    void testOr() {
        //どれか一つが成功した場合、その結果を返すか
        //また、途中で失敗が発生した場合ロールバックされるか
        Parser<Token> first = token(Token.Kind.IF);
        Parser<Token> second = token(Token.Kind.ELSE);
        Parser<Token> third = token(Token.Kind.WHILE);
        TokenStream firstSuccess = TestUtil.toTokenStream("if");
        assertEquals(new ParseSuccess<>(new Token(Token.Kind.IF, "if", "if", 1, 1)),
                or(first, second, third).parse(firstSuccess));
        assertEquals(1, firstSuccess.pos());

        TokenStream secondSuccess = TestUtil.toTokenStream("else");
        assertEquals(new ParseSuccess<>(new Token(Token.Kind.ELSE, "else", "else", 1, 1)),
                or(first, second, third).parse(secondSuccess));
        assertEquals(1, secondSuccess.pos());

        TokenStream thirdSuccess = TestUtil.toTokenStream("while");
        assertEquals(new ParseSuccess<>(new Token(Token.Kind.WHILE, "while", "while", 1, 1)),
                or(first, second, third).parse(thirdSuccess));
        assertEquals(1, thirdSuccess.pos());

        //どれか一つが成功した場合、それより後のものは実行されないか
        TokenStream all = TestUtil.toTokenStream("if else while");
        assertEquals(new ParseSuccess<>(new Token(Token.Kind.IF, "if", "if else while", 1, 1)),
                or(first, second, third).parse(all));
        assertEquals(1, all.pos());

        //全て失敗した場合、最もposが大きいものの失敗が返さるか
        TokenStream empty = new TokenStream(List.of());
        Parser<Token> firstFail = stream -> new ParseFailed<>("test2", 2);
        Parser<Token> secondFail = stream -> new ParseFailed<>("test3", 3);
        Parser<Token> thirdFail = stream -> new ParseFailed<>("test1", 1);
        assertEquals(new ParseFailed<>("test3", 3),
                or(firstFail, secondFail, thirdFail).parse(empty));

        //すべて失敗した場合、ロールバックされるか
        TokenStream rollBack = TestUtil.toTokenStream("1 2 3 4 5");
        Parser<Token> firstRollBack = stream -> {
            stream.advance();
            stream.advance();
            return new ParseFailed<>("test2", 2);
        };
        Parser<Token> secondRollBack = stream -> {
            stream.advance();
            stream.advance();
            stream.advance();
            return new ParseFailed<>("test3", 3);
        };
        Parser<Token> thirdRollBack = stream -> {
            stream.advance();
            return new ParseFailed<>("test1", 1);
        };
        or(firstRollBack, secondRollBack, thirdRollBack).parse(rollBack);
        assertEquals(0, rollBack.pos());

        //例外が発生した場合伝播するか
        TokenStream exception = new TokenStream(List.of());
        Parser<Token> firstException = stream -> {
            throw new RuntimeException("test1");
        };
        Parser<Token> secondException = stream -> {
            throw new RuntimeException("test2");
        };
        Parser<Token> thirdException = stream -> {
            throw new RuntimeException("test3");
        };
        assertThrows(RuntimeException.class,
                () -> or(firstException, secondException, thirdException).parse(exception));

        //引数が空の場合、IllegalArgumentExceptionが投げられるか
        assertThrows(IllegalArgumentException.class, () -> or());

        //引数がnullの場合、NullPointerExceptionが投げられるか
        assertThrows(NullPointerException.class, () -> or((Parser<?>[]) null));

        //可変長引数の要素がnullの場合、NullPointerExceptionが投げられるか
        assertThrows(NullPointerException.class, () -> or((Parser<?>) null));

        //例外が発生した場合、伝播するか
        assertThrows(RuntimeException.class, () -> or(stream -> {
            throw new RuntimeException("test");
        }).parse(new TokenStream(List.of())));
    }

    @Test
    void testMany() {
        //最後まで成功した場合、EOFの手前まで消費されるか
        TokenStream successInAll = TestUtil.toTokenStream("let let let");
        assertEquals(new ParseSuccess<>(
                        List.of(
                                new Token(Token.Kind.LET, "let", "let let let", 1, 1),
                                new Token(Token.Kind.LET, "let", "let let let", 1, 5),
                                new Token(Token.Kind.LET, "let", "let let let", 1, 9)
                        )),
                many(token(Token.Kind.LET)).parse(successInAll));
        assertEquals(3, successInAll.pos());

        //途中で失敗した場合、失敗したものの手前まで消費されるか
        TokenStream successPartially = TestUtil.toTokenStream("let let var");
        assertEquals(new ParseSuccess<>(
                        List.of(
                                new Token(Token.Kind.LET, "let", "let let var", 1, 1),
                                new Token(Token.Kind.LET, "let", "let let var", 1, 5)
                        )),
                many(token(Token.Kind.LET)).parse(successPartially));
        assertEquals(2, successPartially.pos());

        //成功したのに位置が変わらない場合、エラーになるか
        TokenStream notAdvance = TestUtil.toTokenStream("let");
        assertThrows(IllegalStateException.class,
                () -> {
                    many(stream -> new ParseSuccess<>(Token.Kind.LET)).parse(notAdvance);
                });

        //失敗して位置が位置が進められている場合、ParseFailedを返すか
        TokenStream failAdvance = TestUtil.toTokenStream("test");
        assertEquals(new ParseFailed<>("Test fail", 3),
                many(stream -> new ParseFailed<>("Test fail", 3)).parse(failAdvance));

        //一回も成功しない場合、空のリストが変えるか
        TokenStream fail = TestUtil.toTokenStream("var");
        assertEquals(new ParseSuccess<>(List.of()),
                many(token(Token.Kind.LET)).parse(fail));

        //引数がnullの場合、NullPointerExceptionが投げられるか
        assertThrows(NullPointerException.class, () -> many(null));

        //例外が発生した場合、伝播するか
        assertThrows(RuntimeException.class, () -> many(stream -> {
            throw new RuntimeException("test");
        }).parse(new TokenStream(List.of())));
    }

    @Test
    void testSequence() {
        //全て成功した場合
        TokenStream successInAll = TestUtil.toTokenStream("let var if");
        assertEquals(new ParseSuccess<>(
                        List.of(
                                new Token(Token.Kind.LET, "let", "let var if", 1, 1),
                                new Token(Token.Kind.VAR, "var", "let var if", 1, 5),
                                new Token(Token.Kind.IF, "if", "let var if", 1, 9)
                        )),
                sequence(token(Token.Kind.LET), token(Token.Kind.VAR), token(Token.Kind.IF))
                        .parse(successInAll));
        assertEquals(3, successInAll.pos());

        //途中で失敗した場合
        TokenStream fail = TestUtil.toTokenStream("let var for");
        assertEquals(new ParseFailed<>(
                        """
                                [line 1, column 9] Unexpected token: 'for'. Expected: 'IF'
                                let var for
                                        ^
                                """,
                        2
                ),
                sequence(token(Token.Kind.LET), token(Token.Kind.VAR), token(Token.Kind.IF))
                        .parse(fail));
        assertEquals(2, fail.pos());

        //引数がnullの場合、NullPointerExceptionが投げられるか
        assertThrows(NullPointerException.class, () -> sequence((Parser<?>[]) null));
        //可変長引数のどれかの要素がnullの場合、NullPointerExceptionが投げられるか
        assertThrows(NullPointerException.class, () -> sequence((Parser<?>) null));
        //引数が空の場合、IllegalArgumentExceptionが投げられるか
        assertThrows(IllegalArgumentException.class, Parsers::sequence);

        //例外が発生した場合、伝播するか
        assertThrows(RuntimeException.class, () -> sequence(stream -> {
            throw new RuntimeException("test");
        }).parse(new TokenStream(List.of())));
    }

    @Test
    void testLazy() {
        class Counter {
            int count = 0;
        }
        Counter counter = new Counter();

        //毎回実行され、かつ遅延するか
        Parser<Integer> lazied = lazy(() -> {
            counter.count++;
            return stream -> new ParseSuccess<>(counter.count);
        });
        assertEquals(0, counter.count);

        assertEquals(new ParseSuccess<>(1), lazied.parse(new TokenStream(List.of())));
        assertEquals(1, counter.count);

        assertEquals(new ParseSuccess<>(2), lazied.parse(new TokenStream(List.of())));
        assertEquals(2, counter.count);

        //例外がSupplier内で発生した場合、伝播するか
        assertThrows(RuntimeException.class, () -> {
            lazy(() -> {
                throw new RuntimeException("test");
            }).parse(new TokenStream(List.of()));
        });

        //Supplierがnullの場合、NullPointerExceptionが発生するか
        assertThrows(NullPointerException.class, () -> lazy(null));
    }

    @Test
    void testSeparatedBy() {
        //正常に区切られている場合
        TokenStream success = TestUtil.toTokenStream("1, 2");
        assertEquals(new ParseSuccess<>(
                List.of(
                        new Token(Token.Kind.INTEGER, "1", "1, 2", 1, 1),
                        new Token(Token.Kind.INTEGER, "2", "1, 2", 1, 4)
                )
        ), separatedBy(token(Token.Kind.INTEGER), token(Token.Kind.COMMA)).parse(success));
        assertEquals(3, success.pos());

        //一つしかない場合
        TokenStream only = TestUtil.toTokenStream("1");
        assertEquals(new ParseSuccess<>(
                List.of(
                        new Token(Token.Kind.INTEGER, "1", "1", 1, 1)
                )
        ), separatedBy(token(Token.Kind.INTEGER), token(Token.Kind.COMMA)).parse(only));
        assertEquals(1, only.pos());

        //一つも無い場合
        TokenStream empty = TestUtil.toTokenStream("");
        assertEquals(new ParseSuccess<>(List.of()), separatedBy(token(Token.Kind.IDENTIFIER), token(Token.Kind.COMMA)).parse(empty));

        //区切り文字だけが末尾にある場合
        TokenStream onlySeparated = TestUtil.toTokenStream("1, ");
        assertEquals(new ParseFailed<>(
                """
                        [line 1, column 4] Unexpected token: '<EOF>'. Expected: 'INTEGER'
                        1,\s
                           ^
                        """,
                2
        ), separatedBy(token(Token.Kind.INTEGER), token(Token.Kind.COMMA)).parse(onlySeparated));
        assertEquals(1, only.pos());

        //途中で例外が発生した場合、伝播するか
        assertThrows(RuntimeException.class, () -> separatedBy(stream -> {
            throw new RuntimeException("test");
        }, token(Token.Kind.COMMA))
                .parse(new TokenStream(List.of())));
        assertThrows(RuntimeException.class, () -> separatedBy(token(Token.Kind.IDENTIFIER),
                stream -> {
                    throw new RuntimeException("test");
                })
                .parse(TestUtil.toTokenStream("identifier, identifier")));

        // 引数がnullの場合、NullPointerExceptionが発生するか
        assertThrows(NullPointerException.class, () -> separatedBy(null, token(Token.Kind.COMMA)));
        assertThrows(NullPointerException.class, () -> separatedBy(token(Token.Kind.IDENTIFIER), null));
    }

    @Test
    void testRemoveIf() {
        //正しく対象から削除するか
        Parser<List<String>> target = stream -> new ParseSuccess<>(new ArrayList<>(List.of("OK", "OK", "Failure", "OK")));
        assertEquals(new ParseSuccess<>(List.of("OK", "OK", "OK")),
                removeIf(target, s -> s.equalsIgnoreCase("FAILURE")).parse(new TokenStream(List.of())));

        //対象が失敗した場合、失敗するか
        Parser<List<String>> fail = stream -> new ParseFailed<>("failure", 0);
        assertEquals(new ParseFailed<>("failure", 0), removeIf(fail, s -> false).parse(new TokenStream(List.of())));

        //例外が発生した場合、伝播するか
        assertThrows(RuntimeException.class, () -> removeIf(
                stream -> {throw new RuntimeException("test");},
                s -> true
        ).parse(new TokenStream(List.of())));
        assertThrows(RuntimeException.class, () -> removeIf(
                stream -> new ParseSuccess<>(List.of("success")),
                s -> {throw new RuntimeException("test");}
        ).parse(new TokenStream(List.of())));

        //引数がnullの場合、NullPointerExceptionが送出されるか
        assertThrows(NullPointerException.class, () -> removeIf(null, s -> false));
        assertThrows(NullPointerException.class, () -> removeIf(stream -> new ParseSuccess<>(List.of("success")), null));
    }
}