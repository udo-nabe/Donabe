package io.github.udonabe.donabe.parser;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.lexer.Token;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * {@link Parser}のdefaultメソッドだけでは実現が困難な、基本操作を提供する。
 */
final class Parsers {
    private Parsers() {
        throw new AssertionError("Parsers cannot be instantiated.");
    }
    /**
     * 入力した種別のトークンを一つ消費し、一致しなければParseFailedを返す。
     * <p>このメソッドセ生成したパーサーは、成功した場合位置を一つ進め、
     * 失敗した場合は一つも進めずにParseFailedを返す。
     *
     * @param kind 要求するトークン種別。
     * @return 上記の操作を実現するパーサー。
     */
    static Parser<Token> token(Token.Kind kind) {
        return stream -> {
            try {
                return new ParseSuccess<>(stream.consume(kind));
            } catch (CompileException e) {
                return new ParseFailed<>(e.getMessage(), stream.pos());
            }
        };
    }

    /**
     * 引数の複数のパーサーどれかが成功すれば、その結果を返すパーサー。
     * <p>このメソッドで生成したパーサーは、引数の指定順に実行するため、
     * 一度成功した時点でその結果が返される。そのため、成功したらそれ以降のパーサーは実行されない。
     * <p>また、全てのパーサーが失敗した場合、全ての失敗した結果の中で最も深く進んだものの結果を返す。
     *
     * @param parsers 実行するパーサー群。
     * @param <T>     パーサー群共通のスーパークラスまたはインタフェース。
     * @return 成功したパーサーの結果。全て失敗した場合、最も深く進んだパーサーの結果。
     * @throws IllegalArgumentException 引数が空の場合。
     * @throws NullPointerException     {@code parsers}自体、またはその中のどれかがnullの場合。
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T> Parser<T> or(Parser<? extends T>... parsers) {
        if (Objects.requireNonNull(parsers).length == 0) {
            throw new IllegalArgumentException("The argument 'parsers' must not be empty.");
        }
        Arrays.stream(parsers).forEach(Objects::requireNonNull);
        return stream -> {
            List<ParseFailed<T>> fails = new ArrayList<>();
            for (Parser<? extends T> parser : parsers) {
                var fork = stream.fork();
                var result = parser.parse(fork);
                switch (result) {
                    case ParseSuccess<? extends T>(T value) -> {
                        stream.from(fork);
                        return new ParseSuccess<>(value);
                    }
                    case ParseFailed<? extends T> v -> fails.add((ParseFailed<T>) v);
                }
            }
            fails.sort(Comparator.comparingInt(ParseFailed::pos));
            return fails.getLast();
        };
    }

    /**
     * 引数のパーサーを、失敗するまで繰り返すパーサー。
     * <p>もし{@code parser}がParseSuccessを返したのにも関わらず、位置が進まない場合には、
     * IllegalStateExceptionを送出する。
     * そのため、{@link Parser#optional()}を直接の引数にしてはならない。
     * <p>もし{@code parser}がParseFailedを返し、かつ失敗位置が試行開始時の位置と異なる場合、
     * 解析途中で構文エラーが発生したとみなし、ParseFailedを返す。
     *
     * @param parser 繰り返すパーサー。
     * @param <T>    繰り返すパーサーの型。
     * @return 繰り返した結果を、順序を維持してまとめたリスト。
     * @throws IllegalStateException {@code parser}が成功を返したのにも関わらず、位置が変化しない場合。
     * @throws NullPointerException {@code parser}がnullの場合。
     */
    static <T> Parser<List<T>> many(Parser<T> parser) {
        Objects.requireNonNull(parser);
        return stream -> {
            List<T> result = new ArrayList<>();

            outer:
            while (!stream.isAtEnd()) {
                TokenStream fork = stream.fork();
                ParseResult<? extends T> res = parser.parse(fork);

                switch (res) {
                    case ParseSuccess<? extends T>(T value) -> {
                        // 成功したが位置が変わらない場合、無限ループ防止のため例外を出す
                        if (fork.pos() == stream.pos())
                            throw new IllegalStateException("Parser '" + parser + "' did not advance its position.");
                        result.add(value);
                        stream.from(fork);
                    }
                    case ParseFailed<? extends T>(String message, int pos) -> {
                        if (stream.pos() == pos) {
                            break outer;
                        }
                        return new ParseFailed<>(message, pos);
                    }
                }
            }
            return new ParseSuccess<>(result);
        };
    }

    /**
     * 引数のパーサーを順番通りに実行するパーサー。
     * <p>途中のパーサーが失敗した場合、それ以降は実行されない。
     * <p>このパーサーは、概念的には{@link Parser#then(Parser)}を複数つなげたものである。
     * @param parsers 順番に実行されるパーサー群。
     * @return パーサー群の結果を順番にまとめたもの。失敗した場合はその結果。
     * @param <T> リストの型。
     * @throws NullPointerException 可変長引数自体、またはその要素のうちどれかがnullの場合
     * @throws IllegalArgumentException {@code parsers}が空の場合
     */
    @SafeVarargs
    static <T> Parser<List<T>> sequence(Parser<? extends T>... parsers) {
        if (Objects.requireNonNull(parsers).length == 0) {
            throw new IllegalArgumentException("The argument 'parsers' must not be empty.");
        }
        Arrays.stream(parsers).forEach(Objects::requireNonNull);
        return stream -> {
            List<T> result = new ArrayList<>();

            for (Parser<? extends T> parser : parsers) {
                TokenStream fork = stream.fork();
                ParseResult<? extends T> res = parser.parse(fork);

                switch (res) {
                    case ParseSuccess<? extends T>(T value) -> {
                        stream.from(fork);
                        result.add(value);
                    }
                    case ParseFailed<? extends T>(String message, int pos) -> {
                        return new ParseFailed<>(message, pos);
                    }
                }
            }
            return new ParseSuccess<>(result);
        };
    }

    /**
     * 引数のSupplierを遅延評価するパーサー。
     * @param supplier 遅延評価の対象。
     * @return 遅延評価するパーサー。
     * @param <T> パーサーの型。
     * @throws NullPointerException {@code supplier}がnullの場合。
     */
    static <T> Parser<T> lazy(Supplier<Parser<T>> supplier) {
        Objects.requireNonNull(supplier);
        return stream -> supplier.get().parse(stream);
    }

    /**
     * {@code target}を{@code separator}で区切るパーサー。
     * <p>このパーサーは区切り文字が失敗するまで解析し、失敗したら終了とするため、
     * 区切り文字が正しくない場合、その前まで読んで成功となる。
     * そのため、このパーサーを利用するパーサーは、続くトークンで正しいか判定すべきである。
     * <p>なお、{@code target}の解析に失敗した場合は失敗となる。
     * @param target 区切る対象。
     * @param separator 区切るパーサー。
     * @return {@code target}の結果を、順番にまとめたもの。
     * @param <T> {@code target}の型。
     * @throws NullPointerException {@code target}または{@code separator}がnullの場合。
     */
    static <T> Parser<List<T>> separatedBy(Parser<T> target, Parser<?> separator) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(separator);
        return stream -> {
            List<T> result = new ArrayList<>();

            TokenStream fork = stream.fork();
            ParseResult<T> res = target.parse(fork);
            if (res instanceof ParseSuccess<T>(T value)) {
                stream.from(fork);
                result.add(value);
            } else {
                return new ParseSuccess<>(result);
            }

            fork = stream.fork();
            while (separator.parse(fork) instanceof ParseSuccess<?>) {
                res = target.parse(fork);
                switch (res) {
                    case ParseSuccess<T>(T val) -> {
                        stream.from(fork);
                        result.add(val);
                    }
                    case ParseFailed<? extends T>(String message, int pos) -> {
                        return new ParseFailed<>(message, pos);
                    }
                }
            }
            return new ParseSuccess<>(result);
        };
    }

    static <T, C extends Collection<? extends T>> Parser<C> removeIf(Parser<C> target, Predicate<T> pred) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(pred);
        return stream -> {
            var res = target.parse(stream);
            if (res instanceof ParseSuccess<C>(var result)) {
                result.removeIf(pred);
                return new ParseSuccess<>(result);
            } else {
                return res;
            }
        };
    }
}
