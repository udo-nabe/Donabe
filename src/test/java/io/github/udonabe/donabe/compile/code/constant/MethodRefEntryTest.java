package io.github.udonabe.donabe.compile.code.constant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MethodRefEntryTest {

    @Test
    public void testType() {
        MethodRefEntry entry = new MethodRefEntry("hoge");
        assertEquals(entry.type(), ConstantPoolEntry.METHOD_REF_TYPE);
    }

    @Test
    public void testContent() {
        MethodRefEntry hoge = new MethodRefEntry("hoge");
        assertArrayEquals(new byte[]{0x68, 0x6f, 0x67, 0x65}, hoge.content());

        MethodRefEntry hogeUnderscore = new MethodRefEntry("hoge_");
        assertArrayEquals(new byte[]{0x68, 0x6f, 0x67, 0x65, 0x5f}, hogeUnderscore.content());
    }
}
