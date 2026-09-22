package io.github.udonabe.donabe.semantic.type;

import io.github.udonabe.donabe.semantic.CaptureSymbol;
import io.github.udonabe.donabe.semantic.GlobalSymbol;
import io.github.udonabe.donabe.semantic.LocalSymbol;
import io.github.udonabe.donabe.semantic.Symbol;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeCheckerContext {
    private final Deque<Type> returnTypeStack;
    private final List<Map<Symbol, Type>> symbolTypeStack;
    private final Map<GlobalSymbol, Type> globalSymbolType;

    public TypeCheckerContext() {
        returnTypeStack = new ArrayDeque<>();
        symbolTypeStack = new ArrayList<>();
        this.globalSymbolType = new HashMap<>();
    }

    public void pushFunction(Type returnType) {
        returnTypeStack.push(returnType);
        symbolTypeStack.addLast(new HashMap<>());
    }

    public void popFunction() {
        if (returnTypeStack.isEmpty()) {
            throw new IllegalStateException("Could not pop returnType: empty");
        }
        returnTypeStack.pop();
        symbolTypeStack.removeLast();
    }
    
    public void addSymbolType(Symbol symbol, Type type) {
        if (symbol instanceof GlobalSymbol globalSymbol) {
            globalSymbolType.put(globalSymbol, type);
        } else {
            symbolTypeStack.getLast().put(symbol, type);
        }
    }
    
    public Type getSymbolType(Symbol symbol) {
        return switch(symbol) {
            case GlobalSymbol s -> globalSymbolType.get(s);
            case LocalSymbol s -> symbolTypeStack.getLast().get(s);
            case CaptureSymbol s -> {
                int index = symbolTypeStack.size() - s.depth() - 1;
                var parentMap = symbolTypeStack.get(index);
                yield parentMap.get(new LocalSymbol(s.slot()));
            }
        };
    }
    
    public boolean hasSymbolType(Symbol symbol) {
        return globalSymbolType.containsKey(symbol) ||
                symbolTypeStack.getLast().containsKey(symbol);
    }

    public Type currentReturnType() {
        return returnTypeStack.peek();
    }
    
    public boolean isRoot() {
        return returnTypeStack.isEmpty();
    }
}
