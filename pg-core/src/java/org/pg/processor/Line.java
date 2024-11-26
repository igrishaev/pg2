package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class Line extends AProcessor {

    public static final int oid = OID.LINE;

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof org.pg.type.Line l) {
            return l.toByteBuffer();
        } else if (x instanceof String s) {
            return org.pg.type.Line.fromString(s).toByteBuffer();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Line.fromMap(m).toByteBuffer();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Line.fromList(l).toByteBuffer();
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof org.pg.type.Line l) {
            return l.toString();
        } else if (x instanceof String s) {
            return org.pg.type.Line.fromString(s).toSQL();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Line.fromMap(m).toSQL();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Line.fromList(l).toSQL();
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return org.pg.type.Line.fromByteBuffer(bb).toClojure();
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        return org.pg.type.Line.fromString(text).toClojure();
    }
}
