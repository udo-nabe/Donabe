package io.github.udonabe.donabe.semantic.ir;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.udonabe.donabe.semantic.GlobalSymbol;
import io.github.udonabe.donabe.semantic.LocalSymbol;
import io.github.udonabe.donabe.semantic.Symbol;

public class IRGenerateContext {

	private Deque<Set<Symbol>> locals;
	private int labelIndex;

	public IRGenerateContext(Set<GlobalSymbol> globals) {
		locals = new ArrayDeque<>();
		locals.push(globals.stream()
				.map(s -> (Symbol) s)
				.collect(Collectors.toSet()));

		labelIndex = 0;
	}

	public void pushFunction(int currentLocalCount) {
		locals.push(IntStream.range(0, currentLocalCount)
				.mapToObj(i -> new LocalSymbol(i))
				.collect(Collectors.toSet()));
	}

	public void popFunction() {
		if (locals.size() <= 1) {
			throw new IllegalStateException("Cannot call popFunction when not inside a function.");
		}
		locals.pop();
	}

	public Set<Symbol> currentLocals() {
		return locals.peek();
	}

	public boolean shouldUseLocal(int slot) {
		return currentLocals().contains(slot);
	}

	public int nextLabelIndex() {
		return labelIndex++;
	}

	public String nextLabel() {
		return "." + nextLabelIndex();
	}
}
