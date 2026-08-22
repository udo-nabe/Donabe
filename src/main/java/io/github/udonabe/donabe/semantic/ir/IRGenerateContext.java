package io.github.udonabe.donabe.semantic.ir;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

public class IRGenerateContext {
    private Deque<Set<Integer>> locals;
    private int labelIndex;

    public IRGenerateContext() {
        locals = new ArrayDeque<>();
        labelIndex = 0;
    }

    public void pushFunction(Set<Integer> currentLocals) {
        locals.push(currentLocals);
    }
    public void popFunction() {
        if (locals.isEmpty()) {
            throw new IllegalStateException("Cannot call popFunction when not inside a function.");
        }
        locals.pop();
    }
    public boolean inFunction() {
        return !locals.isEmpty();
    }
    public Set<Integer> currentLocals() {
        return locals.peek();
    }
    public boolean isLocal(int slot) {
        return currentLocals().contains(slot);
    }

    public int nextLabelIndex() {
        return labelIndex++;
    }
    public String nextLabel() {
        return "." + nextLabelIndex();
    }
}
