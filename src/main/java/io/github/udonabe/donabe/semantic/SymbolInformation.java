package io.github.udonabe.donabe.semantic;

import java.util.Objects;

public record SymbolInformation(boolean isAssignable, boolean isTemporary) {
    public SymbolInformation(boolean isAssignable) {
        this(isAssignable, false);
    }
}
