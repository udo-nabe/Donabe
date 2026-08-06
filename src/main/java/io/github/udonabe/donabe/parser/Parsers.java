package io.github.udonabe.donabe.parser;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.lexer.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Parsers {
    static Parser<Token> token(Token.Kind kind) {
        return stream -> {
            try {
                return new ParseSuccess<>(stream.consume(kind));
            } catch (CompileException e) {
                return new ParseFailed<>(e.getMessage(), stream.pos());
            }
        };
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T> Parser<T> or(Parser<? extends T>... parsers) {
        return stream -> {
            if (parsers.length == 0) {
                throw new IllegalArgumentException("The argument 'parsers' must not be empty.");
            }
            List<ParseFailed<T>> fails = new ArrayList<>();
            for (Parser<? extends T> parser : parsers) {
                var fork = stream.fork();
                var result = parser.parse(fork);
                if (result instanceof ParseSuccess<? extends T>(T value)) {
                    stream.from(fork);
                    return new ParseSuccess<>(value);
                }
                fails.add((ParseFailed<T>) result);
            }
            fails.sort(Comparator.comparingInt(ParseFailed::pos));
            return fails.getLast();
        };
    }

    static <T> Parser<List<T>> many(Parser<T> parser) {
        return stream -> {
            List<T> result = new ArrayList<>();

            while (!stream.isAtEnd()) {
                TokenStream fork = stream.fork();
                ParseResult<? extends T> res = parser.parse(fork);

                if (res instanceof ParseSuccess<? extends T>(T value)) {
                    // 成功したが位置が変わらない場合、無限ループ防止のため例外を出す
                    if (fork.pos() == stream.pos())
                        throw new IllegalStateException("Parser '" + parser + "' did not advance its position.");
                    result.add(value);
                    stream.from(fork);
                    continue;
                }
                if (res instanceof ParseFailed<? extends T>(String message, int pos)) {
                    if (stream.pos() == pos) {
                        break;
                    }
                    return new ParseFailed<>(message, pos);
                }
            }
            return new ParseSuccess<>(result);
        };
    }

    @SafeVarargs
    static <T> Parser<List<T>> sequence(Parser<? extends T>... parsers) {
        return stream -> {
            List<T> result = new ArrayList<>();

            for (Parser<? extends T> parser : parsers) {
                TokenStream fork = stream.fork();
                ParseResult<? extends T> res = parser.parse(fork);

                if (res instanceof ParseSuccess<? extends T>(T value)) {
                    stream.from(fork);
                    result.add(value);
                } else if (res instanceof ParseFailed<? extends T>(String message, int pos)) {
                    return new ParseFailed<>(message, pos);
                }
            }
            return new ParseSuccess<>(result);
        };
    }

    static <T> Parser<T> lazy(Supplier<Parser<T>> supplier) {
        return stream -> supplier.get().parse(stream);
    }

    static <T> Parser<List<T>> separatedBy(Parser<T> target, Parser<?> separator) {
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
                if (res instanceof ParseSuccess<T>(T val)) {
                    stream.from(fork);
                    result.add(val);
                } else if (res instanceof ParseFailed<? extends T>(String message, int pos)) {
                    return new ParseFailed<>(message, pos);
                }
            }
            return new ParseSuccess<>(result);
        };
    }

    static <T, C extends Collection<? extends T>> Parser<C> removeIf(Parser<C> target, Predicate<T> pred) {
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
