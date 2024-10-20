package org.pg.type.processor.pgvector;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;
import org.pg.codec.CodecParams;
import org.pg.error.PGError;
import org.pg.type.processor.AProcessor;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;
import java.util.Map;

//
// https://github.com/pgvector/pgvector/blob/049972a4a3a04e0f49de73d78915706377035f48/src/sparsevec.c#L552
//
public class Sparsevec extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof Map<?,?> m) {
            final int len = m.size();
            final ByteBuffer bb = ByteBuffer.allocate(4 + 4 + )
            for (Object keyOjb: m.keySet()) {

            }
//            for (Map.Entry<?,?> me: m.entrySet()) {
//                Object keyObj = me.getKey();
//                Object valObj = me.getKey();
//                if (keyObj instanceof Number nKey) {
//                    int key = NumTool.toInteger(nKey);
//                } else {
//                    throw new PGError("key %s is not a number", keyObj);
//                }
//                if (valObj instanceof Number nVal) {
//                    float val = NumTool.toFloat(nVal);
//                } else {
//                    throw new PGError("value %s is not a number", valObj);
//                }
//            }

        } else if (x instanceof Iterable<?> iterable) {
            for (Object item: iterable) {
                if (item instanceof Number n) {

                } else {
                    throw new PGError("item %s is not a number", item);
                }
            }
        } else {
            return binEncodingError(x);
        }
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
    public IPersistentMap decodeTxt(final String text, final CodecParams codecParams) {
        final String[] topParts = text.split("[{}]");
        if (topParts.length != 3) {
            throw new PGError("wrong sparsevec input string: %s", text);
        }
        final String numPart = topParts[1];
        final String[] numPairs = numPart.split(",");

        IPersistentMap result = PersistentHashMap.EMPTY;

        for (String numPair: numPairs) {
            String[] pair = numPair.split(":");
            if (pair.length != 2) {
                throw new PGError("wrong sparsevec input string: %s", text);
            }
            final String idxStr = pair[0];
            final String valStr = pair[1];
            final int idx = Integer.parseInt(idxStr);
            final float val = Float.parseFloat(valStr);
            result = result.assoc(idx, val);
        }

        return result;
    }
}

