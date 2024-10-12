package org.pg.type.processor;

import clojure.lang.Indexed;
import org.pg.codec.*;

import java.nio.ByteBuffer;

public class Array extends AProcessor {

    private final int oid;
    private final int itemOid;

    public Array(final int oid, final int itemOid) {
        this.oid = oid;
        this.itemOid = itemOid;
    }

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Indexed) {
            return ArrayBin.encode(x, oid, itemOid, codecParams);
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Indexed) {
            return ArrayTxt.encode(x, oid, itemOid, codecParams);
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return ArrayBin.decode(bb, codecParams);
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        return ArrayTxt.decode(text, oid, itemOid, codecParams);
    }
}
