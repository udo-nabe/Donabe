package io.github.udonabe.donabe.compile;

import io.github.udonabe.donabe.compile.code.ByteCode;
import io.github.udonabe.donabe.compile.code.EndianUtil;
import io.github.udonabe.donabe.compile.code.section.Section;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class Encoder {
    
    private static final byte[] MAGIC_NUMBER =
    {0x00, 0x44, 0x4E, 0x42};
    private static final byte FILE_VERSION = 0x01;
    private static final byte LANG_VERSION = 0x01;

    public byte[] encode(ByteCode code) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(MAGIC_NUMBER);
            out.write(FILE_VERSION);
            out.write(LANG_VERSION);
            
            for (Section section : code.sections()) {
                out.write(section.type());
                
                byte[] sectionContent = section.content();
                
                out.write(EndianUtil.to4BytesLittleEndian(sectionContent.length));
                out.write(sectionContent);
            }
            
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode.", e);
        }
    }
}
