package io.github.udonabe.donabe.semantic;

public class AnalyzeContext {
    private int functionDepth;

    public AnalyzeContext() {
    }

    public int functionDepth() {
        return functionDepth;
    }

    public void pushFunction() {
        functionDepth++;
    }
    public void popFunction() {
        if (functionDepth <= 0) {
            throw new IllegalStateException("Cannot call popFunction when not inside a function.");
        }
        functionDepth--;
    }
    public boolean inFunction() {
        return functionDepth > 0;
    }
}
