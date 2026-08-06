package io.github.udonabe.donabe.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Scope {
    private final Map<String, SymbolInformation> symbolTable;
    private final Scope parent;

    private Scope(Map<String, SymbolInformation> symbolTable, Scope parent) {
        this.symbolTable = symbolTable;
        this.parent = parent;
    }

    public Scope(Scope parent) {
        this(new HashMap<>(), parent);
    }

    public SymbolInformation get(String key) {
        if (!symbolTable().containsKey(key)) {
            if (parent == null) {
                return null;
            }
            return parent.get(key);
        }
        return symbolTable.get(key);
    }

    public boolean put(String key, SymbolInformation value) {
        if (symbolTable().containsKey(key)) {
            if (symbolTable.get(key).isTemporary()) {
                symbolTable.put(key, value);
                return true;
            }
            return false;
        }
        symbolTable.put(key, value);
        return true;
    }

    public void changeSymbolInfo(String key, SymbolInformation value) {
        symbolTable.put(key, value);
    }

    public Map<String, SymbolInformation> symbolTable() {
        return symbolTable;
    }

    public Scope parent() {
        return parent;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Scope) obj;
        return Objects.equals(this.symbolTable, that.symbolTable) &&
               Objects.equals(this.parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbolTable, parent);
    }

    @Override
    public String toString() {
        return "Scope[" +
               "symbolTable=" + symbolTable + ", " +
               "parent=" + parent + ']';
    }

    public Scope capture() {
        return new Scope(new HashMap<>(this.symbolTable), parent != null ? parent.capture() : null);
    }
}
