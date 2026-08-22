package io.github.udonabe.donabe.ir;

import io.github.udonabe.donabe.ir.instruction.Instruction;

import java.util.List;

public record IRProgram(List<Instruction> instructions) {
}
