package org.pg.codec;

import clojure.lang.Indexed;
import clojure.lang.PersistentVector;
import clojure.lang.IPersistentCollection;
import org.pg.error.PGError;

import clojure.core$assoc_in;
import clojure.lang.RT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Matrix {

    public static Object assocIn(final Object matrix, final int[] path, final Object value) {
        return core$assoc_in.invokeStatic(matrix, path, value);
    }

    public static int[] tail (final int[] array) {
        return Arrays.copyOfRange(array, 1, array.length);
    }

    public static long getTotalCount(final int[] dims) {
        if (dims.length == 0) {
            return 0;
        } else {
            long totalCount = 1;
            for (int dim: dims) {
                totalCount *= dim;
            }
            return totalCount;
        }
    }

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
            final int[] dimsNext = tail(dims);
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

    public static int[] initPath(final int size) {
        final int[] path = new int[size];
        if (size > 0) {
            path[size - 1] = -1;
        }
        return path;
    }

    public static int[] getDims(final Object matrix) {
        final List<Integer> dimsList = new ArrayList<>();
        int size;
        Object target = matrix;
        while (true) {
            if (target instanceof Indexed) {
                size = RT.count(target);
                dimsList.add(size);
                if (size > 0) {
                    target = RT.first(target);
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

    public static Object packElements(final int[] dims, final List<Object> elements) {
        Object matrix = create(dims);
        final int[] path = Matrix.initPath(dims.length);

        for (Object val: elements) {
            Matrix.incPath(dims, path);
            matrix = Matrix.assocIn(matrix, path, val);
        }

        return matrix;
    }

    public static void main(String... args) {
//        Object matrix = create(1, 1, 3, 3, 99);
//        System.out.println(matrix);
//        System.out.println(Arrays.toString(getDims(matrix)));
//        System.out.println(getTotalCount(new int[]{1, 2, 3}));
//        System.out.println(Arrays.toString(initPath(4)));
        // System.out.println(assocVecIn(PersistentVector.EMPTY, new int[]{0, 0, 0}, 42));
        // System.out.println(assocVec(PersistentVector.create(1, 2), 2, 42));


    }

}
