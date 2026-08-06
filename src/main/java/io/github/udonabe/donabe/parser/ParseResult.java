package io.github.udonabe.donabe.parser;

public sealed interface ParseResult<T>
        permits ParseSuccess, ParseFailed {

}
