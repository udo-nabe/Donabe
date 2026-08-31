package io.github.udonabe.donabe.error;

import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.lexer.Token;
import io.github.udonabe.donabe.runtime.context.stack.StackFrame;

public class ErrorUtil {
    public static String makeError(int line, int column, String message, Object... format) {
        String formatted = String.format(message, format);
        return String.format("[line %d, column %d] %s\n", line, column, formatted);
    }
    public static String makeErrorWithSource(int line, int column, String source, String message, Object... format) {
        String error = message + "\n" +
                       source.split("\n", -1)[line - 1] + "\n" +
                       " ".repeat(Math.max(0, column - 1)) + "^";
        return makeError(line, column, error, format);
    }
    public static String makeCompileError(Token token, String unexpected, String expected) {
        String error = "Unexpected token: '%s'. Expected: '%s'\n" +
                       token.lineSource() + "\n" +
                       " ".repeat(Math.max(0, token.column() - 1)) + "^";
        return makeError(token.line(), token.column(), error, unexpected, expected);
    }
    public static String makeError(SourceFileLocation location, String source, String message, Object... format) {
        String error = message + "\n" +
                       source.split("\n", -1)[location.line() - 1] + "\n" +
                       " ".repeat(Math.max(0, location.column() - 1)) + "^";
        return makeError(location.line(), location.column(), error, format);
    }
    public static String makeRuntimeError(StackFrame currentFrame, String message, Object... format) {
        return message.formatted(format) + "\n" +
               StackTraceGenerator.generateStackTrace(currentFrame);
    }
}
