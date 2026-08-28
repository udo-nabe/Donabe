package io.github.udonabe.donabe.semantic.type;

import java.util.ArrayDeque;
import java.util.Deque;

public class TypeCheckerContext {
    private final Deque<Type> returnTypeStack;

    public TypeCheckerContext() {
        returnTypeStack = new ArrayDeque<>();
    }

    public void pushReturnType(Type returnType) {
        returnTypeStack.push(returnType);
    }

    public void popReturnType() {
        if (returnTypeStack.isEmpty()) {
            throw new IllegalStateException("Could not pop returnType: empty");
        }
        returnTypeStack.pop();
    }

    public Type currentReturnType() {
        return returnTypeStack.peek();
    }
}
