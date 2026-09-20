package io.github.udonabe.donabe.compile.code.constant;

import io.github.udonabe.donabe.compile.code.value.CodeValue;
import io.github.udonabe.donabe.compile.code.value.StringCodeValue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValueEntryTest {

    @Test
    public void testType() {
        ValueEntry entry = new ValueEntry(new StringCodeValue("hello"));
        assertEquals(ConstantPoolEntry.VALUE_TYPE, entry.type());
    }

    @Test
    public void testContent() {
        ValueEntry entry = new ValueEntry(new StringCodeValue("hello"));
        assertArrayEquals(new byte[]{
            CodeValue.STRING_TYPE,
            0x05, 0x00, 0x00, 0x00,
            0x68, 0x65, 0x6c, 0x6c, 0x6f
        }, entry.content());
    }
}
