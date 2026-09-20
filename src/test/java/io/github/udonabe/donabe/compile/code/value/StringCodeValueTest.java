package io.github.udonabe.donabe.compile.code.value;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StringCodeValueTest {

    @Test
    public void testType() {
        assertEquals(new StringCodeValue("example").type(), CodeValue.STRING_TYPE);
    }

    @Test
    public void testContent() {
        assertArrayEquals(
                new byte[]{0x68, 0x65, 0x6c, 0x6c, 0x6f},
                new StringCodeValue("hello").content()
        );
        assertArrayEquals(
                new byte[]{},
                new StringCodeValue("").content()
        );
        assertArrayEquals(
                new byte[]{
                    (byte) 0xe3, (byte) 0x81, (byte) 0x93, // こ
                    (byte) 0xe3, (byte) 0x82, (byte) 0x93, // ん
                    (byte) 0xe3, (byte) 0x81, (byte) 0xab, // に
                    (byte) 0xe3, (byte) 0x81, (byte) 0xa1, // ち
                    (byte) 0xe3, (byte) 0x81, (byte) 0xaf  // は
                },
                new StringCodeValue("こんにちは").content()
        );
    }
}
