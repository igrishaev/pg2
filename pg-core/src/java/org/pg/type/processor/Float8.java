package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

public class Float8 extends AProcessor {

    public static final int oid = OID.FLOAT4;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Number n) {
            return BBTool.ofDouble(n.doubleValue());
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return x.toString();
    }

    @Override
    public Double decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return bb.getDouble();
    }

    @Override
    public Double decodeTxt(final String text, final CodecParams codecParams) {
        return Double.parseDouble(text);
    }
}
