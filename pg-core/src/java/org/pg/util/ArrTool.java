package org.pg.util;

import org.pg.error.PGError;

import java.lang.reflect.Array;
import java.util.Arrays;

public final class ArrTool {

    public static Object create(final int... dims) {
        return Array.newInstance(Object.class, dims);
    }

    public static void set(final Object array, final Object value, final int... dims) {
        Object target = array;
        int i = 0;
        for (; i < dims.length - 1; i++) {
            final int dim = dims[i];
            target = Array.get(target, dim);
        }
        Array.set(target, dims[i], value);
    }

    public static Object get(final Object array, final int... dims) {
        Object target = array;
        int i = 0;
        for (; i < dims.length - 1; i++) {
            final int dim = dims[i];
            target = Array.get(target, dim);
        }
        return Array.get(target, dims[i]);
    }

    public static void inc(final int[] dims, final int[] path) {
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

    public static void main (final String[] args) {
        final Object arr = create(2, 2, 2);
        System.out.println(get(arr, 1, 1, 0));
        System.out.println(get(arr, 1, 1, 1));
        set(arr, 42, 1, 1, 1);
        System.out.println(get(arr, 1, 1, 0));
        System.out.println(get(arr, 1, 1, 1));

        final int[] dims = {3, 3, 3};

        int[] path = {0, 0, 0};

        System.out.println(Arrays.toString(path));

//        while (true) {
//            inc(dims, path);
//            System.out.println(Arrays.toString(path));
//        }
    }

}
