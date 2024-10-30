package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;

public class Float4 extends AProcessor {

    public static final int oid = OID.FLOAT4;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        return BBTool.ofFloat(NumTool.toFloat(x));
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return String.valueOf(NumTool.toFloat(x));
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
