package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.codec.PrimitiveBin;
import org.pg.codec.PrimitiveTxt;
import org.pg.enums.OID;

import java.nio.ByteBuffer;

public class Bool extends AProcessor {

    public static final int oid = OID.BOOL;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Boolean b) {
            return PrimitiveBin.encodeBool(b);
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Boolean b) {
            return PrimitiveTxt.encodeBool(b);
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public Boolean decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return PrimitiveBin.decodeBool(bb);
    }

    @Override
    public Boolean decodeTxt(final String string, final CodecParams codecParams) {
        return PrimitiveTxt.decodeBool(string);
    }
}
