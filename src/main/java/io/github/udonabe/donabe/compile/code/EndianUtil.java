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

    public static byte[] to2BytesLittleEndian(int value) {
        if (value > 65535 || value < 0) {
            throw new IllegalArgumentException("The argument 'value' must be a range of u16.");
        }

        return ByteBuffer.allocate(Short.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) value)
                .array();
    }
}
