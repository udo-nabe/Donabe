package io.github.udonabe.donabe.parser;

public record ParseFailed<T>(String message, int pos) implements ParseResult<T> {
}
