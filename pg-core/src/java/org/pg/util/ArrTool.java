package org.pg.util;

import org.pg.error.PGError;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArrTool {

    public static Object create(final int... dims) {
        return Array.newInstance(Object.class, dims);
    }

    public static void setVal(final Object array, final Object value, final int... path) {
        Object target = array;
        int i = 0;
        for (; i < path.length - 1; i++) {
            target = Array.get(target, path[i]);
        }
        Array.set(target, path[i], value);
    }

    public static Object getVal(final Object array, final int... dims) {
        Object target = array;
        int i = 0;
        for (; i < dims.length - 1; i++) {
            final int dim = dims[i];
            target = Array.get(target, dim);
        }
        return Array.get(target, dims[i]);
    }

    public static void incPath(final int[] dims, final int[] path) {
        int i = dims.length - 1;
        boolean isOverflow;
        path[i] += 1;
        for (; i >= 0; i--) {
            isOverflow = dims[i] == path[i];
            if (isOverflow) {
                if (i == 0) {
                    throw new PGError(
                            "path overflow, dims: %s, path: %s",
                            Arrays.toString(dims),
                            Arrays.toString(path)
                    );
                }
                path[i] = 0;
                path[i - 1] += 1;
            }
        }
    }

    public static int[] getDims(final Object array) {
        final List<Integer> dimsList = new ArrayList<>();
        Object target = array;
        while (true) {
            if (target instanceof Object[] oa) {
                dimsList.add(oa.length);
                if (oa.length > 0) {
                    target = oa[0];
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        final int[] dims = new int[dimsList.size()];
        for (int i = 0; i < dims.length; i++) {
            dims[i] = dimsList.get(i);
        }
        return dims;
    }

    public static void main (final String[] args) {
        final Object arr = create(3, 3, 3);
        System.out.println(getVal(arr, 1, 1, 0));
        System.out.println(getVal(arr, 1, 1, 1));
        setVal(arr, 42, 1, 1, 1);
        System.out.println(getVal(arr, 1, 1, 0));
        System.out.println(getVal(arr, 1, 1, 1));
        System.out.println(Arrays.deepToString((Object[])arr));
        System.out.println(Arrays.toString(getDims(create(2, 4, 2))));
    }

}
