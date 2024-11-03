package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;

public class Point extends AProcessor {

    public static final int oid = OID.POINT;

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {

    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {

    }

    @Override
    public org.pg.type.Point decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        final float x = bb.getFloat();
        final float y = bb.getFloat();
        return new org.pg.type.Point(x, y);
    }

    @Override
    public org.pg.type.Point decodeTxt(final String text, final CodecParams codecParams) {

    }
}
