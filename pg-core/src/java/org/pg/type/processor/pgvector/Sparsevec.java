package org.pg.type.processor.pgvector;

import org.pg.codec.CodecParams;
import org.pg.type.SparseVector;
import org.pg.type.processor.AProcessor;

import java.nio.ByteBuffer;

//
// https://github.com/pgvector/pgvector/blob/049972a4a3a04e0f49de73d78915706377035f48/src/sparsevec.c#L552
//
public class Sparsevec extends AProcessor {

    private ByteBuffer encodeSparseVector(final SparseVector sv) {
        final ByteBuffer bb = ByteBuffer.allocate(4 + 4 + 4 + sv.nnz() * 8);
        bb.putInt(sv.dim());
        bb.putInt(sv.nnz());
        bb.putInt(0);
        for (int i = 0; i < sv.nnz(); i++) {
            bb.putInt(sv.getIndex(i));
        }
        for (int i = 0; i < sv.nnz(); i++) {
            bb.putFloat(sv.getValue(i));
        }
        return bb;
    }

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return encodeSparseVector(SparseVector.ofString(s));
        } else if (x instanceof SparseVector sv) {
            return encodeSparseVector(sv);
        } else if (x instanceof Iterable<?> i) {
            return encodeSparseVector(SparseVector.ofIterable(i));
        } else {
            return binEncodingError(x);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else if (x instanceof SparseVector sv) {
            return sv.toString();
        } else if (x instanceof Iterable<?> i) {
            return SparseVector.ofIterable(i).toString();
        } else {
            return txtEncodingError(x);
        }
    }

    @Override
    public SparseVector decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        final int dim = bb.getInt();
        final int nnz = bb.getInt();
        final int ignored = bb.getInt();
        final int[] indexes = new int[nnz];
        final float[] values = new float[nnz];
        for (int i = 0; i < nnz; i++) {
            indexes[i] = bb.getInt();
        }
        for (int i = 0; i < nnz; i++) {
            values[i] = bb.getFloat();
        }
        return new SparseVector(dim, nnz, indexes, values);
    }

    @Override
    public SparseVector decodeTxt(final String text, final CodecParams codecParams) {
        return SparseVector.ofString(text);
    }
}

