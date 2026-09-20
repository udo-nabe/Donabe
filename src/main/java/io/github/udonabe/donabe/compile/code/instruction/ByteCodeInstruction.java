package io.github.udonabe.donabe.compile.code.instruction;

import io.github.udonabe.donabe.compile.code.instruction.operand.Operand;
import java.util.List;

public record ByteCodeInstruction(OpCode opcode, List<Operand> operands) {

}
