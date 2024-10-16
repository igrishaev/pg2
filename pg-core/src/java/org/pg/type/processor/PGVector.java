package org.pg.type.processor;

import clojure.lang.PersistentVector;
import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;

// https://github.com/pgvector/pgvector/blob/049972a4a3a04e0f49de73d78915706377035f48/src/vector.c#L367
public class PGVector extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof Number n) {
            return BBTool.ofLong(NumTool.toLong(n));
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Number n) {
            return String.valueOf(NumTool.toLong(n));
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public PersistentVector decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        final int dim = bb.getShort();
        final int ignored = bb.getShort();
        PersistentVector result = PersistentVector.EMPTY;
        float item;
        for (int i = 0; i < dim; i++) {
            item = bb.getFloat();
            result = result.cons(item);
        }
        return result;
    }

    @Override
    public PersistentVector decodeTxt(final String text, final CodecParams codecParams) {
        text.s

    }
}
