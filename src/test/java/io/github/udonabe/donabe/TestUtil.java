package io.github.udonabe.donabe;

import io.github.udonabe.donabe.lexer.Lexer;

public class TestUtil {
    public static TokenStream toTokenStream(String code) {
        return new Lexer(code).toTokenStream();
    }
}
