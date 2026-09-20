package io.github.udonabe.donabe.compile.code.value;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ListCodeValueTest {

    @Test
    public void testType() {
        assertEquals(new ListCodeValue(List.of()).type(), CodeValue.LIST_TYPE);
    }

    @Test
    public void testContent() {
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00}, new ListCodeValue(List.of()).content());
        assertArrayEquals(
                new byte[]{
                    0x01, 0x00, 0x00, 0x00,
                    CodeValue.INT_TYPE,
                    0x04, 0x00, 0x00, 0x00,
                    0x10, 0x00, 0x00, 0x00
                },
                new ListCodeValue(List.of(
                        new IntCodeValue(0x10)
                )).content());
        assertArrayEquals(
                new byte[]{
                    0x02, 0x00, 0x00, 0x00,
                    CodeValue.INT_TYPE,
                    0x04, 0x00, 0x00, 0x00,
                    0x10, 0x00, 0x00, 0x00,
                    CodeValue.STRING_TYPE,
                    0x01, 0x00, 0x00, 0x00,
                    0x64
                },
                new ListCodeValue(List.of(
                        new IntCodeValue(0x10),
                        new StringCodeValue("d")
                )).content());
    }
}
