package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Int2 extends AProcessor {

    public static final int oid = OID.INT2;

    @Override
    public ByteBuffer encodeBin(final Object value, final CodecParams codecParams) {
        if (value instanceof Number n) {
            return BBTool.ofShort(n.shortValue());
        } else {
            return binEncodingError(value, oid);
        }
    }

    @Override
    public String encodeTxt(final Object value, final CodecParams codecParams) {
        return value.toString();
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
