package org.pg.util;

import java.util.Arrays;

public final class ByteTool {

    public static byte[] concat (final byte[] a, final byte[] b) {
        final byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static byte[] concat (final byte[]... arrays) {
        int len = 0;
        for (byte[] array: arrays) {
            len += array.length;
        }
        final byte[] result = new byte[len];
        int pos = 0;
        for (byte[] array: arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }

    public static byte[] xor (final byte[] a, final byte[] b) {
        final byte[] c = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            c[i] = (byte) (a[i] ^ b[i]);
        }
        return c;
    }

    public static void main(final String... args) {
        final byte[] arr1 = new byte[] {(byte)1, (byte)2, (byte)3};
        final byte[] arr2 = new byte[] {(byte)10, (byte)20, (byte)30};
        System.out.println(Arrays.toString(concat(arr1, arr2, arr1, arr2)));
    }

}
