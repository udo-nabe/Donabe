package io.github.udonabe.donabe.compile.code.constant;

import io.github.udonabe.donabe.compile.code.EndianUtil;
import io.github.udonabe.donabe.compile.code.value.CodeValue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public record ValueEntry(CodeValue value) implements ConstantPoolEntry<CodeValue> {

    @Override
    public byte type() {
        return VALUE_TYPE;
    }

    @Override
    public byte[] content() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(value.type());
            
            byte[] content = value.content();
            
            out.write(EndianUtil.to4BytesLittleEndian(content.length));
            out.write(content);
            
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize ValueEntry.", e);
        }
    }

}
