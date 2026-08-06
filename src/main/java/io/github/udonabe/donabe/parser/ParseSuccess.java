package io.github.udonabe.donabe.parser;

public record ParseSuccess<T>(T value) implements ParseResult<T> {
}
