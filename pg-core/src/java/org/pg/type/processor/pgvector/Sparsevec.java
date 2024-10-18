package org.pg.type.processor.pgvector;

import clojure.lang.PersistentVector;
import org.pg.codec.CodecParams;
import org.pg.error.PGError;
import org.pg.type.processor.AProcessor;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;
import java.util.Iterator;

import clojure.lang.RT;
import org.pg.util.TypeTool;

//
// https://github.com/pgvector/pgvector/blob/049972a4a3a04e0f49de73d78915706377035f48/src/sparsevec.c#L552
//
public class Sparsevec extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        return ByteBuffer.allocate(32);
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return "";
    }

    @Override
    public PersistentVector decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
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
        PersistentVector result = PersistentVector.EMPTY;
        for (int i = 0; i < dim; i++) {
            result = result.cons(0);
        }
        for (int i = 0; i < nnz; i++) {
            int idx = indexes[i];
            float val = values[i];
            result = result.assocN(idx, val);
        }
        return result;
    }

    @Override
    public PersistentVector decodeTxt(final String text, final CodecParams codecParams) {
        return PersistentVector.EMPTY;
    }
}

