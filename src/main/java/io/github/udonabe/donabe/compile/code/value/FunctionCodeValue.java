package io.github.udonabe.donabe.compile.code.value;

import io.github.udonabe.donabe.compile.code.EndianUtil;
import io.github.udonabe.donabe.compile.code.section.CodeSection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record FunctionCodeValue(
        String name,
        List<Integer> paramSlots,
        Set<Integer> locals,
        CodeSection code
        ) implements CodeValue {

    public FunctionCodeValue {
        if (paramSlots.size() >= 0xffff) {
            throw new IllegalArgumentException("Too many params.");
        }
        if (locals.size() >= 0xffff) {
            throw new IllegalArgumentException("Too many locals.");
        }
    }

    private static Logger log = LoggerFactory.getLogger(FunctionCodeValue.class);

    @Override
    public byte type() {
        return CodeValue.FUNCTION_TYPE;
    }

    @Override
    public byte[] content() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            //サイズを明示し、関数名を書く
            byte[] nameBytes = name.getBytes();
            out.write(EndianUtil.to4BytesLittleEndian(nameBytes.length));
            out.write(nameBytes);

            //個数を明示し、引数スロット一覧を順序を保って書く
            out.write(EndianUtil.to2BytesLittleEndian(paramSlots.size()));
            for (int slot : paramSlots) {
                out.write(EndianUtil.to2BytesLittleEndian(slot));
            }

            //個数を明示し、ローカル変数一覧を書く
            out.write(EndianUtil.to2BytesLittleEndian(locals.size()));
            for (int slot : locals) {
                out.write(EndianUtil.to2BytesLittleEndian(slot));
            }

            byte[] codeContent = code.content();

            out.write(EndianUtil.to4BytesLittleEndian(codeContent.length));
            out.write(codeContent);

            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize FunctionCodeValue.", e);
        }
    }
}
