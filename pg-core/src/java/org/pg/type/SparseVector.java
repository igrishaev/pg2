package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;

import java.nio.ByteBuffer;
import java.util.*;

public record SparseVector(
        int dim,
        Map<Integer, Float> index
) implements Counted, IDeref, Iterable<Float>, Indexed {

    private final static float ZERO = (float) 0;

    @Override
    public int count() {
        return dim;
    }

    public int nnz() {
        return index.size();
    }

    public Iterable<Integer> sortedIndexes() {
        return index.keySet().stream().sorted().toList();
    }

    public float getValue(final int idx) {
        return index.getOrDefault(idx, ZERO);
    }

    private boolean fitsDim(final int i) {
        return 0 <= i && i < dim;
    }

    public static SparseVector ofByteBuffer(final ByteBuffer bb) {
        final int dim = bb.getInt();
        final int nnz = bb.getInt();
        final int ignored = bb.getInt();
        final int[] indexes = new int[nnz];
        for (int i = 0; i < nnz; i++) {
            indexes[i] = bb.getInt();
        }
        final Map<Integer, Float> index = new HashMap<>(nnz);
        float f;
        for (int i = 0; i < nnz; i++) {
            f = bb.getFloat();
            index.put(indexes[i], f);
        }
        return new SparseVector(dim, index);
    }

    public static SparseVector ofString(final String text) {
        final String[] partsRaw = text.split("[{}:,/]");
        final List<String> partsClear = new ArrayList<>();
        for (String part: partsRaw) {
            String partClear = part.strip();
            if (!partClear.isEmpty()) {
                partsClear.add(partClear);
            }
        }
        final int len = partsClear.size();
        final Map<Integer, Float> index = new HashMap<>();
        int i = 0;
        int dim = 0;
        int idx = 0;
        float val;
        for (String partClear: partsClear) {
            if (i == len - 1) {
                dim = Integer.parseInt(partClear);
            } else if (i % 2 == 0) {
                idx = Integer.parseInt(partClear);
            } else {
                val = Float.parseFloat(partClear);
                index.put(idx-1, val);
            }
            i++;
        }
        return new SparseVector(dim, index);
    }

    public static SparseVector ofIterable(final Iterable<?> iterable) {
        int dim = 0;
        Map<Integer, Float> index = new HashMap<>();
        float f;
        for (Object item: iterable) {
            f = RT.floatCast(item);
            if (f != 0) {
                index.put(dim, f);
            }
            dim++;
        }
        return new SparseVector(dim, index);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        int nnz = index.size();
        int i = 0;
        for (int idx: sortedIndexes()) {
            float val = getValue(idx);
            sb.append(idx + 1);
            sb.append(":");
            sb.append(val);
            if (i < nnz - 1) {
                sb.append(",");
            }
            i++;
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
        for (Map.Entry<Integer, Float> me: index.entrySet()) {
            int idx = me.getKey();
            float val = me.getValue();
            result = result.assocN(idx, val);
        }
        return result;
    }

    @Override
    public Object deref() {
        return PersistentHashMap.create(
                KW.dim, dim,
                KW.nnz, index.size(),
                KW.index, PersistentHashMap.create(index)
        );
    }

    @Override
    public Iterator<Float> iterator() {
        return new Iterator<>() {

            private int i = 0;

            @Override
            public boolean hasNext() {
                return fitsDim(i);
            }

            @Override
            public Float next() {
                final float f = index.getOrDefault(i, ZERO);
                i++;
                return f;
            }
        };
    }

    @Override
    public Object nth(final int i) {
        if (fitsDim(i)) {
            return index.getOrDefault(i, ZERO);
        } else {
            throw new IndexOutOfBoundsException("index is out of range: " + i);
        }
    }

    @Override
    public Object nth(final int i, final Object notFound) {
        if (fitsDim(i)) {
            return index.getOrDefault(i, ZERO);
        } else {
            return notFound;
        }
    }
}
