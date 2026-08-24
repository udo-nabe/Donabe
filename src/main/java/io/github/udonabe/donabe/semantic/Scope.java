package io.github.udonabe.donabe.semantic;

import io.github.udonabe.donabe.ast.expr.Identifier;

import java.util.*;

public final class Scope {
    private final Map<String, SymbolInformation> symbolTable;
    private final Map<String, Integer> identifierIds;
    private final Scope parent;
    private final List<Scope> children;
    private int childPos;

    public static Scope generateRoot() {
        return new Scope(null);
    }

    private Scope(Map<String, SymbolInformation> symbolTable, Map<String, Integer> identifierIds, Scope parent) {
        this.symbolTable = symbolTable;
        this.identifierIds = identifierIds;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.childPos = 0;
    }

    private Scope(Scope parent) {
        this(new HashMap<>(), new HashMap<>(), parent);
    }

    public void resetChildPos() {
        childPos = 0;
        children.forEach(Scope::resetChildPos);
    }

    public Scope newChild() {
        Scope child = new Scope(this);
        children.add(child);
        return child;
    }

    public Scope nextChildScope() {
        try {
            return children.get(childPos++);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalStateException(e);
        }
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
                return -1;
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

    public Map<String, SymbolInformation> symbolTable() {
        return Map.copyOf(symbolTable);
    }

    public Scope parent() {
        return parent;
    }

    public List<Scope> children() {
        return List.copyOf(children);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Scope scope)) return false;
        return childPos == scope.childPos && Objects.equals(symbolTable, scope.symbolTable) && Objects.equals(identifierIds, scope.identifierIds) && Objects.equals(parent, scope.parent) && Objects.equals(children, scope.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbolTable, identifierIds, parent, children, childPos);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Scope{");
        sb.append("symbolTable=").append(symbolTable);
        sb.append(", identifierIds=").append(identifierIds);
        sb.append(", children=").append(children);
        sb.append(", childPos=").append(childPos);
        sb.append('}');
        return sb.toString();
    }
}
