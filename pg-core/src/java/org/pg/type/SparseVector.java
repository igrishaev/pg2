package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;
import clojure.java.api.Clojure;
import org.pg.error.PGError;
import org.pg.util.ArrayTool;
import org.pg.util.NumTool;

import java.util.ArrayList;
import java.util.List;

public record SparseVector(
        int dim,
        int nnz,
        int[] indexes,
        float[] values
) implements Counted, IDeref {

    private final static IFn fnZipmap = Clojure.var("clojure.core", "zipmap");
    private final static float ZERO = (float) 0;

    public int getIndex(final int i) {
        return indexes[i];
    }

    public float getValue(final int i) {
        return values[i];
    }

    @Override
    public int count() {
        return dim;
    }

    public static SparseVector ofString(final String text) {
        final String[] topParts = text.split("[{}]");
        if (topParts.length != 3) {
            throw new PGError("wrong sparsevec input string: %s", text);
        }
        final String numPart = topParts[1];
        final String dimPart = topParts[2];
        final String dimStr = dimPart.replace("/", "").strip();
        final int dim = Integer.parseInt(dimStr);
        final String[] numPairs = numPart.split(",");
        List<Integer> indexesList = new ArrayList<>();
        List<Float> valuesList = new ArrayList<>();
        int nnz = 0;
        for (String numPair: numPairs) {
            String[] pair = numPair.split(":");
            if (pair.length != 2) {
                throw new PGError("wrong sparsevec input string: %s", text);
            }
            final String idxStr = pair[0];
            final String valStr = pair[1];
            final int idx = Integer.parseInt(idxStr.strip());
            final float val = Float.parseFloat(valStr.strip());
            indexesList.add(idx - 1);
            valuesList.add(val);
            nnz++;
        }
        int[] indexes = ArrayTool.toIntArray(indexesList);
        float[] values = ArrayTool.toFloatArray(valuesList);
        return new SparseVector(dim, nnz, indexes, values);
    }

    public static SparseVector ofIterable(final Iterable<?> iterable) {
        int dim = 0;
        int nnz = 0;
        List<Integer> indexesList = new ArrayList<>();
        List<Float> valuesList = new ArrayList<>();
        float f;
        for (Object item: iterable) {
            dim++;
            f = NumTool.toFloat(item);
            if (f != 0) {
                nnz++;
                indexesList.add(dim + 1);
                valuesList.add(f);
            }
        }
        int[] indexes = ArrayTool.toIntArray(indexesList);
        float[] values = ArrayTool.toFloatArray(valuesList);
        return new SparseVector(dim, nnz, indexes, values);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < nnz; i++) {
            int idx = indexes[i];
            float val = values[i];
            sb.append(idx + 1);
            sb.append(":");
            sb.append(val);
            if (i < nnz - 1) {
                sb.append(",");
            }
        }
        sb.append("}/");
        sb.append(dim);
        return sb.toString();
    }

    @SuppressWarnings("unused")
    public PersistentVector toVector() {
        PersistentVector result = PersistentVector.EMPTY;
        for (int i = 0; i < dim; i++) {
            result = result.cons(ZERO);
        }
        for (int i = 0; i < nnz; i++) {
            int idx = indexes[i];
            float val = values[i];
            result = result.assocN(idx, val);
        }
        return result;
    }

    @Override
    public Object deref() {
        return PersistentHashMap.create(
                KW.dim, dim,
                KW.nnz, nnz,
                KW.index, fnZipmap.invoke(indexes, values)
        );
    }
}
