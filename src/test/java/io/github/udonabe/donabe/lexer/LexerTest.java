package io.github.udonabe.donabe.lexer;

import io.github.udonabe.donabe.CompileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LexerTest {
    @DisplayName("数値関連のテスト")
    @Test
    void testNumber() {
        //正常に整数を解釈できるか
        Lexer lexer = new Lexer("""
                123456789
                """);
        var integer = lexer.getTokens().getFirst();
        assertEquals(Token.Kind.INTEGER, integer.kind());
        assertEquals("123456789", integer.lexeme());
        assertEquals(1, integer.column());
    }

    @DisplayName("文字列関連のテスト")
    @Test
    void testString() {
        //空白などがある文字列を、"を取って解釈できるか
        Lexer lexer = new Lexer("""
                "Hello, World"
                """);
        var string = lexer.getTokens().getFirst();
        assertEquals(Token.Kind.STRING, string.kind());
        assertEquals("Hello, World", string.lexeme());
        assertEquals(1, string.column());
    }

    @DisplayName("識別子のテスト")
    @Test
    void testIdentifier() {
        //正しい識別子を解釈できるか
        Lexer lexer = new Lexer("""
                hELlo123
                """);
        var identifier = lexer.getTokens().getFirst();
        assertEquals(Token.Kind.IDENTIFIER, identifier.kind());
        assertEquals("hELlo123", identifier.lexeme());
        assertEquals(1, identifier.column());

        //アンダーバーで始まる識別子を解釈できるか
        lexer = new Lexer("__abcE1");
        identifier = lexer.getTokens().getFirst();
        assertEquals(Token.Kind.IDENTIFIER, identifier.kind());
        assertEquals("__abcE1", identifier.lexeme());

        //数字始まりにすると、全体が識別子と扱われなくなるか
        lexer = new Lexer("12aabc");
        identifier = lexer.getTokens().getFirst();
        assertEquals(Token.Kind.INTEGER, identifier.kind());
        assertEquals(Token.Kind.IDENTIFIER, lexer.getTokens().get(1).kind());
    }

    @DisplayName("空白をスキップするテスト")
    @Test
    void testSkipWhiteSpace() {
        //空白、タブ、CR、LFを全てスキップできるか
        Lexer lexer = new Lexer("""
                abc   123\t(\r;
                aaa
                """);
        var tokens = lexer.getTokens();
        var tokenKinds = tokens.stream().map(Token::kind).toList();
        assertIterableEquals(List.of(
                Token.Kind.IDENTIFIER,
                Token.Kind.INTEGER,
                Token.Kind.LPAREN,
                Token.Kind.SEMICOLON,
                Token.Kind.IDENTIFIER,
                Token.Kind.EOF
        ), tokenKinds);
    }

    @DisplayName("コメントをスキップするテスト")
    @Test
    void testSkipComments() {
        //どこにあるどの種類のコメントでもスキップできるか
        Lexer lexer = new Lexer("""
                // Comment
                /* Block Comment */
                // var print;
                /**/""");
        var tokens = lexer.getTokens();
        var tokenKinds = tokens.stream().map(Token::kind).toList();
        assertIterableEquals(List.of(Token.Kind.EOF), tokenKinds);

        // コメントを入れた場合でも、行番号が正しくなるか
        lexer = new Lexer("// Comment\na");
        var token = lexer.getTokens().getFirst();
        assertEquals(2, token.line());

        lexer = new Lexer("/* Comment\n this is block. */\na");
        token = lexer.getTokens().getFirst();
        assertEquals(3, token.line());

        // ブロックコメント内に/や*が単体であっても、正しく解釈されるか
        assertDoesNotThrow(() -> {
            new Lexer("/* * / */");
        });

        //ネストしたブロックコメントがエラーになるか
        assertThrows(CompileException.class, () -> {
            new Lexer("/* /**/ */");
        });

        //ブロックコメントが/でおわり、閉じられていない場合エラーになるか
        assertThrows(CompileException.class, () -> {
            new Lexer("/* /");
        });

        //閉じていないブロックコメントがエラーになるか
        assertThrows(CompileException.class, () -> {
            new Lexer("/* a");
        });

        assertThrows(CompileException.class, () -> {
            new Lexer("/* *");
        });
    }

    @DisplayName("記号のテスト")
    @Test
    void testSymbols() {
        //記号が正しく解釈できるか
        Lexer lexer = new Lexer("""
                == /= = ,""");
        var tokens = lexer.getTokens();
        var tokenKinds = tokens.stream().map(Token::kind).toList();
        assertIterableEquals(List.of(
                Token.Kind.EQUAL,
                Token.Kind.SLASH_ASSIGN,
                Token.Kind.ASSIGN,
                Token.Kind.COMMA,
                Token.Kind.EOF
        ), tokenKinds);

        //存在しない記号でエラーになるか
        assertThrows(CompileException.class, () -> {
            new Lexer("@");
        });
    }

    @DisplayName("キーワードの")
    @Test
    void testKeyword() {
        //キーワードが正しくトークンに変換されるか
        Lexer lexer = new Lexer("""
                let if in func for
                """);
        var tokens = lexer.getTokens();
        var tokenKinds = tokens.stream().map(Token::kind).toList();
        assertIterableEquals(List.of(
                Token.Kind.LET,
                Token.Kind.IF,
                Token.Kind.IN,
                Token.Kind.FUNC,
                Token.Kind.FOR,
                Token.Kind.EOF
        ), tokenKinds);

        //キーワードが部分的に含まれている識別子が、識別子として変換されるか
        lexer = new Lexer("""
                letter if_then else0if into variable
                """);
        tokens = lexer.getTokens();
        tokenKinds = tokens.stream().map(Token::kind).toList();
        assertIterableEquals(List.of(
                Token.Kind.IDENTIFIER,
                Token.Kind.IDENTIFIER,
                Token.Kind.IDENTIFIER,
                Token.Kind.IDENTIFIER,
                Token.Kind.IDENTIFIER,
                Token.Kind.EOF
        ), tokenKinds);
    }

    @DisplayName("基本的な動作のテスト")
    @Test
    void testLex() {
        //トークン数が一致するか
        Lexer lexer = new Lexer("let a b c\nlet a = 0;");
        var tokens = lexer.getTokens();
        assertEquals(10, tokens.size());

        //トークン種別が全て正しいか
        var tokenKinds = tokens.stream().map(Token::kind).toList();
        assertIterableEquals(List.of(
                Token.Kind.LET,
                Token.Kind.IDENTIFIER,
                Token.Kind.IDENTIFIER,
                Token.Kind.IDENTIFIER,
                Token.Kind.LET,
                Token.Kind.IDENTIFIER,
                Token.Kind.ASSIGN,
                Token.Kind.INTEGER,
                Token.Kind.SEMICOLON,
                Token.Kind.EOF
        ), tokenKinds);

        //lineSourceが正しいか
        var firstHalf = tokens.subList(0, 4);
        var lastHalf = tokens.subList(5, tokens.size());

        for (Token token : firstHalf) {
            assertEquals("let a b c", token.lineSource());
        }
        for (Token token : lastHalf) {
            assertEquals("let a = 0;", token.lineSource());
        }
    }
}
