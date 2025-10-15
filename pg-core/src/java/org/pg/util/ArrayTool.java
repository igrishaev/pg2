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

    public static void main(final String... args) {
        System.out.println(indexOf(new String[] {"abc", null, "ccc"}, null));

    }

}
