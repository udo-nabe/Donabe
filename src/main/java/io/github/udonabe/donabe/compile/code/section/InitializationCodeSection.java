package io.github.udonabe.donabe.compile.code.section;

import io.github.udonabe.donabe.compile.code.instruction.ByteCodeInstruction;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public record InitializationCodeSection(List<ByteCodeInstruction> instructions) implements Section {

    @Override
    public byte type() {
        return CODE_SECTION_TYPE;
    }

    @Override
    public byte[] content() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (ByteCodeInstruction instruction : instructions) {
                out.write(instruction.opcode().opcode());
                for (var operand : instruction.operands()) {
                    out.write(operand.content());
                }
            }

            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize CodeSection.", e);
        }
    }

}
