package org.pg.util;

import java.util.List;

public class ArrayTool {

    public static int[] toIntArray(final List<Integer> list) {
        final int len = list.size();
        final int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    public static float[] toFloatArray(final List<Float> list) {
        final int len = list.size();
        final float[] result = new float[len];
        for (int i = 0; i < len; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

}
