package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;

public class Int4 extends AProcessor {

    public static final int oid = OID.INT4;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        return BBTool.ofInt(NumTool.toInteger(x));
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return String.valueOf(NumTool.toInteger(x));
    }

    @Override
    public Integer decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return bb.getInt();
    }

    @Override
    public Integer decodeTxt(final String text, final CodecParams codecParams) {
        return Integer.parseInt(text);
    }
}
