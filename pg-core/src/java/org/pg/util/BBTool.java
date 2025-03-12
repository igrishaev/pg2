package org.pg.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class BBTool {

    public static ByteBuffer ofShort (final short value) {
        final ByteBuffer buf = ByteBuffer.allocate(2);
        buf.putShort(value);
        return buf;
    }

    public static ByteBuffer ofInt (final int value) {
        final ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(value);
        return buf;
    }

    public static ByteBuffer ofString (final String value, final Charset charset) {
        final byte[] bytes = value.getBytes(charset);
        return ByteBuffer.wrap(bytes);
    }

    public static ByteBuffer ofLong (final long value) {
        final ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(value);
        return buf;
    }

    public static ByteBuffer ofFloat (final float value) {
        final ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putFloat(value);
        return buf;
    }

    public static ByteBuffer ofDouble (final double value) {
        final ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putDouble(value);
        return buf;
    }

    public static ByteBuffer ofUUID (final UUID uuid) {
        final ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return buf;
    }

    public static ByteBuffer ofBool (final boolean value) {
        final ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(value ? (byte)1 : (byte)0);
        return buf;
    }

    public static String getString (final ByteBuffer bb, final int len) {
        final int pos = bb.position();
        return new String(bb.array(), pos, len, StandardCharsets.UTF_8);
    }

    public static String getCString (final ByteBuffer buf, final Charset charset) {
        final int pos = buf.position();
        int len = 0;
        while (buf.get(pos + len) != 0) {
            len++;
        }
        skip(buf, len + 1);
        return new String(buf.array(), pos, len, charset);
    }

    public static String getRestString (final ByteBuffer buf, Charset charset) {
        return new String(
                buf.array(),
                buf.arrayOffset() + buf.position(),
                buf.remaining(),
                charset
        );
    }

    public static byte[] getRestBytes (final ByteBuffer buf) {
        final int size = buf.limit() - buf.position();
        final byte[] bytes = new byte[size];
        buf.get(bytes);
        return bytes;
    }

    public static void skip (final ByteBuffer buf, final int offset) {
        buf.position(buf.position() + offset);
    }

}
