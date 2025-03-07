package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;

public class Int2 extends AProcessor {

    public static final int oid = OID.INT2;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        return BBTool.ofShort(NumTool.toShort(x));
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return String.valueOf(NumTool.toShort(x));
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
