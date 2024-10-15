package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.util.NumTool;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

public class Float4 extends AProcessor {

    public static final int oid = OID.FLOAT4;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Number n) {
            return BBTool.ofFloat(NumTool.toFloat(n));
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Number n) {
            return String.valueOf(NumTool.toFloat(n));
        } else {
            return txtEncodingError(x, oid);
        }
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
