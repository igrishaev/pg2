package org.pg.util;

import java.util.List;

public class CollTool {
    public static int[] toArray(final List<Integer> list) {
        final int len = list.size();
        final int[] result = new int[len];
        Integer item;
        for (int i = 0; i < len; i++) {
            item = list.get(i);
            if (item != null) {
                result[i] = item;
            }
        }
        return result;
    }
}
