package io.github.udonabe.donabe.compile.code.constant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MethodRefEntryTest {

    @Test
    public void testType() {
        MemberRefEntry entry = new MemberRefEntry("hoge");
        assertEquals(entry.type(), ConstantPoolEntry.METHOD_REF_TYPE);
    }

    @Test
    public void testContent() {
        MemberRefEntry hoge = new MemberRefEntry("hoge");
        assertArrayEquals(new byte[]{0x68, 0x6f, 0x67, 0x65}, hoge.content());

        MemberRefEntry hogeUnderscore = new MemberRefEntry("hoge_");
        assertArrayEquals(new byte[]{0x68, 0x6f, 0x67, 0x65, 0x5f}, hogeUnderscore.content());
    }
}
