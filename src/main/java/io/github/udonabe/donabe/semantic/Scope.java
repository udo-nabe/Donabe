package io.github.udonabe.donabe.semantic;

import io.github.udonabe.donabe.ast.expr.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Scope {
    private final Map<String, SymbolInformation> symbolTable;
    private final Map<String, Integer> identifierIds;
    private final Scope parent;

    private Scope(Map<String, SymbolInformation> symbolTable, Map<String, Integer> identifierIds, Scope parent) {
        this.symbolTable = symbolTable;
        this.identifierIds = identifierIds;
        this.parent = parent;
    }

    public Scope(Scope parent) {
        this(new HashMap<>(), new HashMap<>(), parent);
    }

    public SymbolInformation get(String key) {
        Objects.requireNonNull(key);
        if (!symbolTable.containsKey(key)) {
            if (parent == null) {
                return null;
            }
            return parent.get(key);
        }
        return symbolTable.get(key);
    }

    public int getId(String identifier) {
        Objects.requireNonNull(identifier);
        if (!identifierIds.containsKey(identifier)) {
            if (parent == null) {
                return Identifier.UNRESOLVED_ID;
            }
            return parent.getId(identifier);
        }
        return identifierIds.get(identifier);
    }

    public boolean put(String key, SymbolInformation value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (symbolTable.containsKey(key)) {
            if (symbolTable.get(key).isTemporary()) {
                symbolTable.put(key, value);
                return true;
            }
            return false;
        }
        symbolTable.put(key, value);
        return true;
    }

    public boolean putId(String identifier, int id) {
        Objects.requireNonNull(identifier);
        if (identifierIds.containsKey(identifier)) {
            return false;
        }
        identifierIds.put(identifier, id);
        return true;
    }

    public boolean isDeclared(String key) {
        return symbolTable.containsKey(key);
    }

    public boolean changeSymbolInfo(String key, SymbolInformation value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (symbolTable.containsKey(key)) {
            symbolTable.put(key, value);
            return true;
        }
        return false;
    }

    public Map<String, SymbolInformation> symbolTable() {
        return Map.copyOf(symbolTable);
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
        return new Scope(new HashMap<>(this.symbolTable), new HashMap<>(this.identifierIds), parent != null ? parent.capture() : null);
    }
}
