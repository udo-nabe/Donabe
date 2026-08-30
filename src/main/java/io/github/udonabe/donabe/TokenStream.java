package io.github.udonabe.donabe;

import io.github.udonabe.donabe.error.ErrorUtil;
import io.github.udonabe.donabe.lexer.Token;

import java.util.List;

public class TokenStream {
    private final List<Token> tokens;
    private int pos;

    private TokenStream(List<Token> tokens, int pos) {
        this.tokens = tokens;
        this.pos = pos;
    }

    public TokenStream(List<Token> tokens) {
        this(List.copyOf(tokens), 0);
    }

    public Token advance() {
        return tokens.get(Math.min(pos++, tokens.size() - 1));
    }
    public Token peek() {
        return tokens.get(Math.min(pos, tokens.size() - 1));
    }
    public Token previous() {
        return tokens.get(Math.max(pos - 1, 0));
    }

    public boolean match(Token.Kind kind) {
        if (peek().kind() == kind) {
            advance();
            return true;
        }
        return false;
    }
    public Token consume(Token.Kind kind) {
        if (peek().kind() == kind) {
            return advance();
        }
        throw new CompileException(ErrorUtil.makeCompileError(peek(), peek().lexeme(), kind.name()));
    }
    public boolean isAtEnd() {
        return peek().kind() == Token.Kind.EOF;
    }

    public int pos() {
        return pos;
    }

    public TokenStream fork() {
        return new TokenStream(this.tokens, pos);
    }

    public void from(TokenStream fork) {
        if (this.tokens != fork.tokens) {
            throw new IllegalArgumentException("The argument 'fork' was not forked from this TokenStream.");
        }
        this.pos = fork.pos;
    }
}
