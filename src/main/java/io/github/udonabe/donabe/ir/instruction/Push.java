package io.github.udonabe.donabe.ir.instruction;

import io.github.udonabe.donabe.runtime.value.RuntimeValue;

public record Push(RuntimeValue<?> value) implements Instruction {
}
