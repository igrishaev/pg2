package org.pg.util;

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

    public static void main(final String... args) {
        System.out.println(indexOf(new String[] {"abc", null, "ccc"}, null));

    }

}
