package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;

public class Int8 extends AProcessor {

    public static final int oid = OID.INT8;

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
    public Long decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return bb.getLong();
    }

    @Override
    public Long decodeTxt(final String text, final CodecParams codecParams) {
        return Long.parseLong(text);
    }
}
