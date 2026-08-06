package io.github.udonabe.donabe.parser;

import io.github.udonabe.donabe.TokenStream;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.function.Function;

/**
 * パーサーコンビネーターの中核となる関数インタフェース。
 * <p>この関数インタフェースを組み合わせ、複雑な解析を実現する。
 *
 * @param <T> 成功時、このパーサーが{@link ParseSuccess}にラップして返す型。
 *            通常{@link io.github.udonabe.donabe.lexer.Token}や{@link io.github.udonabe.donabe.ast.ASTNode}のサブタイプを使用するが、
 *            任意の型にしても問題ない。
 */
@FunctionalInterface
public interface Parser<T> {
    /**
     * {@link TokenStream}を必要なだけ消費し、{@link ParseResult}を返す。
     * <p>このメソッド内で送出された例外は重大なエラー、または実装上のバグとして扱われるため、パーサーコンビネーター内で捕捉してはならず、
     * 外部で捕捉すべきである。また、解析失敗を表したい場合は、通常{@link ParseFailed}を使用し、
     * 何か重大な障害が発生したときのみ、例外を送出すべきである。
     * <p>そのため、パーサーコンビネーター内で例外が発生する可能性のある処理をする場合は、
     * 上記に則って適切に処理する必要がある。
     *
     * @param in 入力のトークン列。
     * @return パース結果。
     * @see TokenStream
     * @see ParseResult
     */
    ParseResult<T> parse(TokenStream in);

    /**
     * 自分自身を別の型のパーサーに変換するパーサーを返す。
     *
     * @param map 変換方法。なお、これをパーサーが適用する最中に発生した{@link RuntimeException}は捕捉され、同じメッセージを持つ{@link ParseFailed}に変換される。
     * @param <R> 変換先の型。
     * @return 引数mapを適用し、{@code <R>}に変換するパーサー。
     */
    default <R> Parser<R> map(Function<T, R> map) {
        return stream -> {
            ParseResult<T> result = this.parse(stream);

            if (result instanceof ParseSuccess<T>(T value)) {
                try {
                    return new ParseSuccess<>(map.apply(value));
                } catch (RuntimeException e) {
                    return new ParseFailed<>(e.getMessage(), stream.pos());
                }
            }
            ParseFailed<T> failed = ((ParseFailed<T>) result);
            return new ParseFailed<>(failed.message(), failed.pos());
        };
    }

    /**
     * 自分自身と、引数のパーサー両方で順に解析するパーサーを返す。
     * <p>このメソッドは、どちらかのパースに失敗した場合、その{@link ParseFailed}を返す。なお、順番にパースするため、
     * 自分自身のパースに失敗したら{@code next}はパースされない。
     *
     * @param next 自分自身の次に実行されるパーサー。
     * @param <R>  {@code next}の返す型。
     * @return 自分自身と{@code next}のパース結果を{@link Pair}でひとまとめにしたもの。
     */
    default <R> Parser<Pair<T, R>> then(Parser<R> next) {
        return stream -> {
            ParseResult<T> first = this.parse(stream);

            if (first instanceof ParseFailed<T>(String message, int pos)) {
                return new ParseFailed<>(message, pos);
            }

            ParseResult<R> second = next.parse(stream);
            if (second instanceof ParseFailed<R>(String message, int pos)) {
                return new ParseFailed<>(message, pos);
            }

            return new ParseSuccess<>(Pair.of(((ParseSuccess<T>) first).value(), ((ParseSuccess<R>) second).value()));
        };
    }

    /**
     * 自分自身と、引数のパーサー両方で順に解析し、自分自身の結果を捨てるパーサーを返す。
     * <p>このメソッドは、どちらかのパースに失敗した場合、その{@link ParseFailed}を返す。なお、順番にパースするため、
     * 自分自身のパースに失敗したら{@code next}はパースされない。
     *
     * @param next 自分自身の次に実行されるパーサー。
     * @param <R>  {@code next}の返す型。
     * @return {@code next}のパース結果。
     */
    default <R> Parser<R> to(Parser<R> next) {
        return stream -> {
            ParseResult<T> first = this.parse(stream);

            if (first instanceof ParseFailed<T>(String message, int pos)) {
                return new ParseFailed<>(message, pos);
            }

            ParseResult<R> second = next.parse(stream);
            if (second instanceof ParseFailed<R>(String message, int pos)) {
                return new ParseFailed<>(message, pos);
            }

            return second;
        };
    }

    /**
     * 自分自身と、引数のパーサー両方で順に解析し、{@code next}の結果を捨てるパーサーを返す。
     * <p>このメソッドは、どちらかのパースに失敗した場合、その{@link ParseFailed}を返す。なお、順番にパースするため、
     * 自分自身のパースに失敗したら{@code next}はパースされない。
     *
     * @param next 自分自身の次に実行されるパーサー。
     * @return 自分自身のパース結果。
     */
    default Parser<T> skip(Parser<?> next) {
        return stream -> {
            ParseResult<T> first = this.parse(stream);

            if (first instanceof ParseFailed<T>(String message, int pos)) {
                return new ParseFailed<>(message, pos);
            }

            ParseResult<?> second = next.parse(stream);
            if (second instanceof ParseFailed<?>(String message, int pos)) {
                return new ParseFailed<>(message, pos);
            }

            return first;
        };
    }

    /**
     * 自分自身を、{@code left}と{@code right}で挟み込んだパーサーを返す。
     * <p>このメソッドは、{@code left}→自分自身→{@code right}の順でパースするパーサーを返すため、先に実行されたパーサーが失敗した場合、
     * 後のパーサーは実行されない。
     *
     * @param left  自分自身の前に実行されるパーサー。
     * @param right 自分自身の次に実行されるパーサー。
     * @return 自分自身のパース結果。
     *
     *
     */
    default Parser<T> between(Parser<?> left, Parser<?> right) {
        return left.to(this).skip(right);
    }

    /**
     * 自分自身を、必須ではないパーサーに変換したものを返す。
     * <p>このメソッドで変換したパーサーはバックトラックを行うため、失敗しても元の位置を維持する。
     * そのため、{@link Parsers#many(Parser)}の直接の引数としてはならない。
     * @return 自分自身のパースに成功した場合は、それを{@link Optional}でラップしたもの。失敗した場合は、{@code Optional.empty}。
     * @see Parsers#many(Parser)
     */
    default Parser<Optional<T>> optional() {
        return stream -> {
            TokenStream fork = stream.fork();
            ParseResult<T> res = this.parse(fork);

            if (res instanceof ParseSuccess<T>(T value)) {
                stream.from(fork);
                return new ParseSuccess<>(Optional.of(value));
            } else {
                return new ParseSuccess<>(Optional.empty());
            }
        };
    }
}
