package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Int8 extends AProcessor {

    public static final int oid = OID.INT8;

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof Number n) {
            return BBTool.ofLong(n.longValue());
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return x.toString();
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
