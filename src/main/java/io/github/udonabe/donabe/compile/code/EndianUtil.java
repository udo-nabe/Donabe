package io.github.udonabe.donabe.compile.code;

public final class EndianUtil {

    public static byte[] to4BytesLittleEndian(int value) {
        return new byte[] {
            (byte) (value & 0xFF),
            (byte) (value >>> 8 & 0xFF),
            (byte) (value >>> 16 & 0xFF),
            (byte) (value >>> 24 & 0xFF),
        };      
    }

    public static byte[] to2BytesLittleEndian(int value) {
        if (value > 65535 || value < 0) {
            throw new IllegalArgumentException("The argument 'value' must be a range of u16.");
        }

        return new byte[] {
            (byte) (value & 0xFF),
            (byte) (value >>> 8 & 0xFF),
        };      
    }
}
