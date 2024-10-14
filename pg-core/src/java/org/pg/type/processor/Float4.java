package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

public class Float4 extends AProcessor {

    public static final int oid = OID.FLOAT4;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Number n) {
            return BBTool.ofFloat(n.floatValue());
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return x.toString();
    }

    @Override
    public Float decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return bb.getFloat();
    }

    @Override
    public Float decodeTxt(final String text, final CodecParams codecParams) {
        return Float.parseFloat(text);
    }
}
