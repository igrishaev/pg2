package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;

public class Float8 extends AProcessor {

    public static final int oid = OID.FLOAT4;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        return BBTool.ofDouble(NumTool.toDouble(x));
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return String.valueOf(NumTool.toDouble(x));
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
