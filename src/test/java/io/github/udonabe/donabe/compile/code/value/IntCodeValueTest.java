package io.github.udonabe.donabe.compile.code.value;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntCodeValueTest {
    
    @Test
    public void testType() {
        assertEquals(new IntCodeValue(3).type(), CodeValue.INT_TYPE);
    }

    @Test
    public void testContent() {
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00}, new IntCodeValue(0).content());
        assertArrayEquals(new byte[]{0x2a, 0x00, 0x00, 0x00}, new IntCodeValue(42).content());
        assertArrayEquals(new byte[]{0x78, 0x56, 0x34, 0x12}, new IntCodeValue(0x12345678).content());
        assertArrayEquals(new byte[]{(byte) 0x88, (byte) 0xa9, (byte) 0xcb, (byte) 0xed}, new IntCodeValue(-0x12345678).content());
    }
}
