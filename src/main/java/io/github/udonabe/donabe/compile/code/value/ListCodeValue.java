package io.github.udonabe.donabe.compile.code.value;

import io.github.udonabe.donabe.compile.code.EndianUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public record ListCodeValue(List<CodeValue> values) implements CodeValue {
    
    @Override
    public byte type() {
        return CodeValue.LIST_TYPE;
    }

    @Override
    public byte[] content() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(EndianUtil.to4BytesLittleEndian(values.size()));
            
            for (var value : values) {
                out.write(value.type());
                
                byte[] valueContent = value.content();
                
                out.write(EndianUtil.to4BytesLittleEndian(valueContent.length));
                out.write(valueContent);
            }
            throw new UnsupportedOperationException("Writing ListCodeValue is unsupported currently.");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize ListCodeValue.", e);
        }
    }
}
