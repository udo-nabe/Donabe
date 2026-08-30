package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.runtime.context.stack.StackFrame;

public class InterpreterException extends RuntimeException {
    private final StackFrame occurredFrame;

    public InterpreterException(String message, StackFrame occurredFrame) {
        super(message);
        this.occurredFrame = occurredFrame;
    }

    public InterpreterException(String message, Throwable cause, StackFrame occurredFrame) {
        super(message, cause);
        this.occurredFrame = occurredFrame;
    }

    public InterpreterException(Throwable cause, StackFrame occurredFrame) {
        super(cause);
        this.occurredFrame = occurredFrame;
    }

    public StackFrame occurredFrame() {
        return occurredFrame;
    }
}
