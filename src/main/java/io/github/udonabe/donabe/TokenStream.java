package io.github.udonabe.donabe;

import io.github.udonabe.donabe.error.ErrorUtil;
import io.github.udonabe.donabe.lexer.Token;

import java.util.List;

/**
 * Lexer→Parserのデータの受け渡しを行う。
 */
public class TokenStream {
    private final List<Token> tokens;
    private int pos;

    private TokenStream(List<Token> tokens, int pos) {
        this.tokens = List.copyOf(tokens);
        this.pos = pos;
    }

    public TokenStream(List<Token> tokens) {
        this(tokens, 0);
    }

    /**
     * 現在位置が指しているトークンを取得し、一つ進める。
     * 終端より後ろを指している場合は、末尾の要素を返す。
     * 
     * @return 現在位置のトークン。
     */
    public Token advance() {
        return tokens.get(Math.min(pos++, tokens.size() - 1));
    }

    /**
     * 現在位置が指しているトークンを取得する。
     * 終端より後ろを指している場合は、末尾の要素を返す。
     * 
     * @return 現在位置のトークン。
     */
    public Token peek() {
        return tokens.get(Math.min(pos, tokens.size() - 1));
    }

    /**
     * {@code kind}の種類のトークンを消費する。
     * 種類が違えば例外を投げる。
     * 
     * @param kind 期待されるトークンの種別。
     * @return 実際に消費されたトークン。
     * @throws CompileException 現在のトークンの種別が、期待と異なる場合。
     *                          この場合、トークンは消費されない。
     */
    public Token consume(Token.Kind kind) {
        if (peek().kind() == kind) {
            return advance();
        }
        throw new CompileException(ErrorUtil.makeCompileError(peek(), peek().lexeme(), kind.name()));
    }

    /**
     * 現在位置が終端以降に達しているか判定する。
     * 
     * @return 終端以降に達している場合は{@code true}、
     *         そうでない場合は{@code false}。
     */
    public boolean isAtEnd() {
        return peek().kind() == Token.Kind.EOF;
    }

    public int pos() {
        return pos;
    }

    /**
     * 自分自身から分岐した{@code TokenStream}を生成する。
     * {@code tokens}は不変なため共有され、{@code pos}は現在の自分自身の値がコピーされる。
     * 分岐元と分岐先の操作は、互いに影響を与えない。
     * 
     * @return 分岐した{@code TokenStream}。
     */
    public TokenStream fork() {
        return new TokenStream(this.tokens, pos);
    }

    /**
     * 分岐した{@code TokenStream}の位置を取り込む。
     * 
     * @param fork 分岐先。自身から{@link #fork()}されたものでなければならない。
     * @throws IllegalArgumentException 自身の{@link #tokens}と、{@code fork}の{@link #tokens}が同一でない場合。
     */
    public void commit(TokenStream fork) {
        if (this.tokens != fork.tokens) {
            throw new IllegalArgumentException("The argument 'fork' was not forked from this TokenStream.");
        }
        this.pos = fork.pos;
    }
}
