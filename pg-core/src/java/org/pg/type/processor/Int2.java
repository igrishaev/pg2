package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;

public class Int2 extends AProcessor {

    public static final int oid = OID.INT2;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Number n) {
            return BBTool.ofShort(NumTool.toShort(n));
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Number n) {
            return String.valueOf(NumTool.toShort(n));
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public Short decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return bb.getShort();
    }

    @Override
    public Short decodeTxt(final String text, final CodecParams codecParams) {
        return Short.parseShort(text);
    }
}
