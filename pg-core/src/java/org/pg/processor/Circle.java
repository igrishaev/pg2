package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class Circle extends AProcessor {

    public static final int oid = OID.CIRCLE;

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof org.pg.type.Circle b) {
            return b.toByteBuffer();
        } else if (x instanceof String s) {
            return org.pg.type.Circle.fromString(s).toByteBuffer();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Circle.fromMap(m).toByteBuffer();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Circle.fromList(l).toByteBuffer();
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof org.pg.type.Circle b) {
            return b.toString();
        } else if (x instanceof String s) {
            return org.pg.type.Circle.fromString(s).toString();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Circle.fromMap(m).toString();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Circle.fromList(l).toString();
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public org.pg.type.Circle decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return org.pg.type.Circle.fromByteBuffer(bb);
    }

    @Override
    public org.pg.type.Circle decodeTxt(final String text, final CodecParams codecParams) {
        return org.pg.type.Circle.fromString(text);
    }
}
