package io.github.udonabe.donabe.compile.code.section;

import io.github.udonabe.donabe.compile.code.EndianUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

public record IdentifiersSection(Set<Integer> slots) implements Section {

    @Override
    public byte type() {
        return IDENTIFIERS_SECTION_TYPE;
    }

    @Override
    public byte[] content() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(EndianUtil.to4BytesLittleEndian(slots.size()));
            
            for (var slot : slots) {
                out.write(EndianUtil.to4BytesLittleEndian(slot));
            }

            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize IdentifiersSection.", e);
        }
    }

}
