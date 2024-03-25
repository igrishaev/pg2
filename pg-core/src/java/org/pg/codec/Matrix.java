package org.pg.codec;

import clojure.lang.PersistentVector;
import org.pg.error.PGError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Matrix {

    public static PersistentVector create(final int... dims) {
        if (dims.length == 0) {
            return null;
        } else if (dims.length == 1) {
            PersistentVector result = PersistentVector.EMPTY;
            final int dim = dims[0];
            for (int i = 0; i < dim; i++) {
                result = result.cons(null);
            }
            return result;
        } else {
            PersistentVector result = PersistentVector.EMPTY;
            final int dim = dims[0];
            final int[] dimsNext = Arrays.copyOfRange(dims, 1, dims.length);
            for (int i = 0; i < dim; i++) {
                result = result.cons(create(dimsNext));
            }
            return result;
        }
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


    public static void main(String... args) {
        System.out.println(create(1, 3, 3));
    }

}
