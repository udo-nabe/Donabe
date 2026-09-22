package io.github.udonabe.donabe.compile.code.section;

import io.github.udonabe.donabe.compile.code.EndianUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public record GlobalIdentifiersSection(Set<String> globals) implements Section {

    public GlobalIdentifiersSection {
        if (globals.size() > 0xFFFF) {
            throw new IllegalArgumentException("Identifiers are too many.");
        }
    }

    @Override
    public byte type() {
        return GLOBAL_IDENTIFIERS_SECTION_TYPE;
    }

    @Override
    public byte[] content() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(EndianUtil.to2BytesLittleEndian(globals.size()));

            for (var value : globals) {
                byte[] content = value.getBytes(StandardCharsets.UTF_8);

                out.write(EndianUtil.to4BytesLittleEndian(content.length));
                out.write(content);
            }

            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize ConstantPoolSection.", e);
        }
    }
}
