package io.github.udonabe.donabe.semantic;


public record SymbolInformation(boolean isAssignable, boolean isTemporary) {
    public SymbolInformation(boolean isAssignable) {
        this(isAssignable, false);
    }
}
