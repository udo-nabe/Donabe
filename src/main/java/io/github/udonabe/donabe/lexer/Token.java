package io.github.udonabe.donabe.lexer;

import java.util.regex.Pattern;

public record Token(Kind kind, String lexeme, String lineSource, int line, int column) {
    public enum Kind {
        // Keywords
        LET,
        VAR,
        TRUE,
        FALSE,
        IF,
        ELSE,
        WHILE,
        FOR,
        FUNC,
        RETURN,
        IN,
        LAZY,
        // Identifier
        IDENTIFIER,
        // Values
        INTEGER,
        STRING,
        // Operators
        ASSIGN,
        PLUS,
        MINUS,
        ASTERISK,
        SLASH,
        EXCLAMATION,
        INCREMENT,
        DECREMENT,
        PLUS_ASSIGN,
        MINUS_ASSIGN,
        ASTERISK_ASSIGN,
        SLASH_ASSIGN,

        EQUAL,
        LESS,
        GREATER,
        LESS_EQUAL,
        GREATER_EQUAL,
        // Brackets
        LPAREN,
        RPAREN,
        LBRACE,
        RBRACE,
        LBRACKET,
        RBRACKET,
        // Others
        SEMICOLON,
        COMMA,
        EOF
    }
}
