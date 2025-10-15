package org.pg.util;

import java.nio.charset.Charset;

public class ArrayTool {

    // don't care about nulls far now
    public static int indexOf(final Object[] array, final Object v) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(v)) {
                return i;
            }
        }
        return -1;
    }

    public static int readInt(final byte[] bytes, final int[] off) {
        final int value = ((bytes[off[0]] & 0xFF) << 24) |
               ((bytes[off[0] + 1] & 0xFF) << 16) |
               ((bytes[off[0] + 2] & 0xFF) << 8 ) |
               ((bytes[off[0] + 3] & 0xFF));
        off[0] += 4;
        return value;
    }

    public static int readInt(final byte[] bytes, final int off) {
        return ((bytes[off] & 0xFF) << 24) |
                ((bytes[off + 1] & 0xFF) << 16) |
                ((bytes[off + 2] & 0xFF) << 8 ) |
                ((bytes[off + 3] & 0xFF));
    }

    public static short readShort(final byte[] bytes, final int off) {
        return (short) (
                ((bytes[off] & 0xFF) << 8 ) |
                ((bytes[off + 1] & 0xFF))
        );
    }

    public static short readShort(final byte[] bytes, final int[] off) {
        final short value = (short) (
                ((bytes[off[0]] & 0xFF) << 8 ) |
                ((bytes[off[0] + 1] & 0xFF))
        );
        off[0] += 2;
        return value;
    }

    public static void skip(final int len, final int[] off) {
        off[0] += len;
    }

    public static String readCString(final byte[] bytes, final int[] off, final Charset charset) {
        final int pos = off[0];
        int len = 0;
        while (bytes[pos + len] != 0) {
            len++;
        }
        final String value = new String(bytes, pos, len, charset);
        off[0] += len + 1;
        return value;
    }

    public static void main(final String... args) {
        System.out.println(readShort(new byte[] {-1, -1, 127, 127}, new int[] {0}));
        System.out.println(indexOf(new String[] {"abc", null, "ccc"}, null));

    }

}
