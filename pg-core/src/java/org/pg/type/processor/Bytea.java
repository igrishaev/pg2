package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.codec.PrimitiveTxt;
import org.pg.enums.OID;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Bytea extends AProcessor {

    public static final int oid = OID.BYTEA;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof ByteBuffer bb) {
            return bb;
        } else if (x instanceof byte[] ba) {
            return ByteBuffer.wrap(ba);
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof byte[] ba) {
            return PrimitiveTxt.encodeBytea(ba);
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public byte[] decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return BBTool.getRestBytes(bb);
    }

    @Override
    public byte[] decodeTxt(final String string, final CodecParams codecParams) {
        return PrimitiveTxt.decodeBytea(string);
    }
}
