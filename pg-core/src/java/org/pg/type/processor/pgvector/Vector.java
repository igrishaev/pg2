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
// https://github.com/pgvector/pgvector/blob/049972a4a3a04e0f49de73d78915706377035f48/src/vector.c#L367
//
public class Vector extends AProcessor {

    private static float toFloat(final Object x) {
        if (x instanceof Number n) {
            return NumTool.toFloat(n);
        } else {
            throw new PGError("item is not a number: %s", TypeTool.repr(x));
        }
    }

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof Iterable<?> i) {
            final int count = RT.count(i);
            ByteBuffer bb = ByteBuffer.allocate(2 + 2 + count * 4);
            bb.putShort(NumTool.toShort(count));
            bb.putShort((short)0); // ignored
            for (Object item: i) {
                bb.putFloat(toFloat(item));
            }
            return bb;
        } else {
            return binEncodingError(x);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Iterable<?> i) {
            final Iterator<?> iterator = i.iterator();
            final StringBuilder sb = new StringBuilder();
            sb.append('[');
            Object item;
            while (iterator.hasNext()) {
                item = iterator.next();
                sb.append(toFloat(item));
                if (iterator.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append(']');
            return sb.toString();
        } else {
            return txtEncodingError(x);
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
        final int len = text.length();
        // skip [] and split by comma
        final String[] items = text.substring(1, (len - 1)).split("\\s*,\\s*");
        PersistentVector result = PersistentVector.EMPTY;
        for (String item: items) {
            result = result.cons(Float.parseFloat(item));
        }
        return result;
    }
}
