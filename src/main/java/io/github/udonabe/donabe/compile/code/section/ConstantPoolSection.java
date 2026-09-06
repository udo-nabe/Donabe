package io.github.udonabe.donabe.compile.code.section;

import io.github.udonabe.donabe.compile.code.EndianUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import io.github.udonabe.donabe.compile.code.constant.ConstantPoolEntry;

public record ConstantPoolSection(List<ConstantPoolEntry<?>> pool) implements Section {

    @Override
    public byte type() {
        return CONSTANT_POOL_SECTION_TYPE;
    }

    @Override
    public byte[] content() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (var value : pool) {
                out.write(value.type());

                byte[] content = value.content();

                out.write(EndianUtil.to4BytesLittleEndian(content.length));
                out.write(content);
            }

            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize ConstantPoolSection.", e);
        }
    }

}
