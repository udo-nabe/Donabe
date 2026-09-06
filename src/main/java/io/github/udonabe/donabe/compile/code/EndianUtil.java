package io.github.udonabe.donabe.compile.code;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class EndianUtil {
    public static byte[] to4BytesLittleEndian(int value) {
        return ByteBuffer.allocate(Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
    }
}
