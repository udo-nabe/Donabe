package io.github.udonabe.donabe.compile.code.section;

import io.github.udonabe.donabe.compile.code.EndianUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import io.github.udonabe.donabe.compile.code.constant.ConstantPoolEntry;

public record ConstantPoolSection(List<ConstantPoolEntry<?>> pool) implements Section {

    public ConstantPoolSection {
        if (pool.size() > 0xFFFF) {
            throw new IllegalArgumentException("Constant Pool is too many.");
        }
        pool = List.copyOf(pool);
    }

    @Override
    public byte[] content() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(EndianUtil.to2BytesLittleEndian(pool.size()));
            
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
