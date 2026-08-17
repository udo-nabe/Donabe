package io.github.udonabe.donabe.lexer;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.TokenStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;

public class Lexer {
    private static final Pattern INTEGER_PATTERN = Pattern.compile("[0-9]+");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"\\n]*)\"");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern LINE_COMMENT_PATTERN = Pattern.compile("^//[^\n]*(\n|$)");
    private static final Map<String, Token.Kind> KEYWORDS = Map.ofEntries(
            entry("let", Token.Kind.LET),
            entry("var", Token.Kind.VAR),
            entry("true", Token.Kind.TRUE),
            entry("false", Token.Kind.FALSE),
            entry("if", Token.Kind.IF),
            entry("else", Token.Kind.ELSE),
            entry("while", Token.Kind.WHILE),
            entry("for", Token.Kind.FOR),
            entry("func", Token.Kind.FUNC),
            entry("return", Token.Kind.RETURN),
            entry("in", Token.Kind.IN)
    );
    private static final Map<String, Token.Kind> OPERATOR2 = Map.ofEntries(
            entry("==", Token.Kind.EQUAL),
            entry("<=", Token.Kind.LESS_EQUAL),
            entry(">=", Token.Kind.GREATER_EQUAL),
            entry("++", Token.Kind.INCREMENT),
            entry("--", Token.Kind.DECREMENT),
            entry("+=", Token.Kind.PLUS_ASSIGN),
            entry("-=", Token.Kind.MINUS_ASSIGN),
            entry("*=", Token.Kind.ASTERISK_ASSIGN),
            entry("/=", Token.Kind.SLASH_ASSIGN)
    );
    private static final Map<String, Token.Kind> OPERATOR1 = Map.ofEntries(
            entry("=", Token.Kind.ASSIGN),
            entry("+", Token.Kind.PLUS),
            entry("-", Token.Kind.MINUS),
            entry("*", Token.Kind.ASTERISK),
            entry("/", Token.Kind.SLASH),
            entry("(", Token.Kind.LPAREN),
            entry(")", Token.Kind.RPAREN),
            entry("{", Token.Kind.LBRACE),
            entry("}", Token.Kind.RBRACE),
            entry("[", Token.Kind.LBRACKET),
            entry("]", Token.Kind.RBRACKET),
            entry(";", Token.Kind.SEMICOLON),
            entry("!", Token.Kind.EXCLAMATION),
            entry("<", Token.Kind.LESS),
            entry(">", Token.Kind.GREATER),
            entry(",", Token.Kind.COMMA)
    );

    private final String source;
    private final List<Token> tokens;
    private final String[] lines;
    private int pos;
    private int column;
    private int line;

    public Lexer(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
        this.tokens = new ArrayList<>();
        this.lines = source.split("\n", -1);
        tokenize();
    }

    public TokenStream toTokenStream() {
        return new TokenStream(tokens);
    }

    List<Token> getTokens() {
        return List.copyOf(tokens);
    }

    private void tokenize() {
        while (true) {
            Token token = scanToken();
            tokens.add(token);
            if (token.kind() == Token.Kind.EOF) {
                break;
            }
        }
    }

    private Token scanToken() {
        skipIgnored();

        if (pos >= source.length()) {
            return new Token(Token.Kind.EOF, "<EOF>", lines[line - 1], line, column);
        }
        char ch = source.charAt(pos);

        Token result;
        if (Character.isDigit(ch)) {
            result = readNumber();
        } else if (ch == '"') {
            result = readString();
        } else if (Character.isLetter(ch) || ch == '_') {
            result = readIdentifier();
        } else {
            result = readSymbol();
        }
        return result;
    }

    private void skipIgnored() {
        while (true) {
            skipWhiteSpace();
            String twoChars = source.substring(pos, Math.min(source.length(), pos + 2));
            if (twoChars.equals("//")) {
                skipLineComment();
                continue;
            }
            if (twoChars.equals("/*")) {
                skipBlockComment();
                continue;
            }
            break;
        }
    }

    private void skipWhiteSpace() {
        while (pos < source.length()) {
            char ch = source.charAt(pos);

            if (ch == '\n') {
                lineFeed();
            }

            if (ch != ' ' && ch != '\r' && ch != '\n' && ch != '\t') {
                break;
            }

            posIncrement(1);
        }
    }

    private void skipLineComment() {
        Matcher commentMatcher = LINE_COMMENT_PATTERN.matcher(source.substring(pos));
        commentMatcher.lookingAt();
        int len = commentMatcher.end();
        posIncrement(len);
        lineFeed();
    }

    private void skipBlockComment() {
        posIncrement(2);
        while (pos < source.length()) {
            char ch = source.charAt(pos);

            if (ch == '\n') {
                lineFeed();
            }

            if (ch == '/') {
                if (pos + 1 >= source.length()) break;
                if (source.charAt(pos + 1) == '*') {
                    throw new CompileException(ErrorUtil.makeErrorWithSource(line, column, source, "ブロックコメントはネストできません。"));
                }
            }

            if (ch == '*') {
                if (pos + 1 >= source.length()) break;
                if (source.charAt(pos + 1) == '/') {
                    posIncrement(2);
                    return;
                }
            }
            posIncrement(1);
        }
        throw new CompileException(ErrorUtil.makeErrorWithSource(line, column, source, "ブロックコメントが閉じられていません。"));
    }

    private Token readNumber() {
        int startLine = line;
        int startColumn = column;

        Matcher integer = INTEGER_PATTERN.matcher(getCurrentSource());
        integer.lookingAt();

        String match = integer.group();
        posIncrement(match.length());

        return new Token(
                Token.Kind.INTEGER,
                match,
                lines[startLine - 1],
                startLine,
                startColumn
        );
    }

    private Token readString() {
        int startLine = line;
        int startColumn = column;

        Matcher string = STRING_PATTERN.matcher(getCurrentSource());
        string.lookingAt();

        String match = string.group(1);
        posIncrement(string.group().length());
        return new Token(Token.Kind.STRING, match, lines[line - 1], startLine, startColumn);
    }

    private Token readIdentifier() {
        int startLine = line;
        int startColumn = column;

        Matcher identifier = IDENTIFIER_PATTERN.matcher(getCurrentSource());
        identifier.lookingAt();

        String match = identifier.group();
        posIncrement(match.length());

        if (KEYWORDS.containsKey(match)) {
            return new Token(KEYWORDS.get(match), match, lines[line - 1], startLine, startColumn);
        }
        return new Token(Token.Kind.IDENTIFIER, match, lines[line - 1], startLine, startColumn);
    }

    private Token readSymbol() {
        int startLine = line;
        int startColumn = column;

        String s = source.substring(pos, Math.min(source.length(), pos + 2)).trim();

        if (OPERATOR2.containsKey(s)) {
            posIncrement(2);
            return new Token(OPERATOR2.get(s), s, lines[line - 1], startLine, startColumn);
        }

        String s1 = s.substring(0, 1);
        if (OPERATOR1.containsKey(s1)) {
            posIncrement(1);
            return new Token(OPERATOR1.get(s1), s1, lines[line - 1], startLine, startColumn);
        }
        throw new CompileException(ErrorUtil.makeError(startLine, startColumn, "Unknown token: '%s'", s));
    }

    private String getCurrentSource() {
        return source.substring(pos);
    }

    private void lineFeed() {
        line++;
        column = 1;
    }

    private void posIncrement(int increment) {
        pos += increment;
        column += increment;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Lexer{");
        sb.append("source='").append(source).append('\'');
        sb.append(", tokens=").append(tokens);
        sb.append(", pos=").append(pos);
        sb.append(", line=").append(line);
        sb.append('}');
        return sb.toString();
    }
}
