package io.github.udonabe.donabe.semantic.resolve;

import java.util.ArrayDeque;
import java.util.Deque;

class ResolveContext {
	private final Deque<Integer> localSlotCounts;

	public ResolveContext() {
		this.localSlotCounts = new ArrayDeque<>();
	}

	public void pushFunction() {
		localSlotCounts.push(0);
	}

	public int popFunction() {
		return localSlotCounts.pop();
	}

	public int issueID() {
		if (localSlotCounts.isEmpty()) {
			throw new IllegalStateException("Cannot issue an ID in the root scope.");
		}
		int current = localSlotCounts.pop();
		localSlotCounts.push(current + 1);
		return current;
	}

	public boolean isRoot() {
		return localSlotCounts.isEmpty();
	}
}
