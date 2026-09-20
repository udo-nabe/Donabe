package io.github.udonabe.donabe.compile.code.instruction.operand;

import io.github.udonabe.donabe.compile.code.EndianUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OperandTest {

    @Test
    public void testContent() {
        Operand operand = new OperandImpl(0x123456);
        assertArrayEquals(new byte[]{0x56, 0x34, 0x12, 0x00}, operand.content());
    }

    public record OperandImpl(int value) implements Operand {

        @Override
        public byte[] content() {
            return EndianUtil.to4BytesLittleEndian(value);
        }
    }
}
