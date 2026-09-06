package io.github.udonabe.donabe.compile.code.value;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BoolCodeValueTest {
    
    @Test
    public void testType() {
        assertEquals(new BoolCodeValue(true).type(), CodeValue.BOOL_TYPE);
    }

    @Test
    public void testContent() {
        assertArrayEquals(new BoolCodeValue(true).content(), new byte[]{0x01});
        assertArrayEquals(new BoolCodeValue(false).content(), new byte[]{0x00});
    }
}
