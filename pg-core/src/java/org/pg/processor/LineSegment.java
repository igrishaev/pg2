package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class LineSegment extends AProcessor {

    public static final int oid = OID.LSEG;

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof org.pg.type.LineSegment ls) {
            return ls.toByteBuffer();
        } else if (x instanceof String s) {
            return org.pg.type.LineSegment.fromString(s).toByteBuffer();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.LineSegment.fromMap(m).toByteBuffer();
        } else if (x instanceof List<?> l) {
            return org.pg.type.LineSegment.fromList(l).toByteBuffer();
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof org.pg.type.LineSegment ls) {
            return ls.toSQL();
        } else if (x instanceof String s) {
            return org.pg.type.LineSegment.fromString(s).toSQL();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.LineSegment.fromMap(m).toSQL();
        } else if (x instanceof List<?> l) {
            return org.pg.type.LineSegment.fromList(l).toSQL();
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return org.pg.type.LineSegment.fromByteBuffer(bb).toClojure();
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        return org.pg.type.LineSegment.fromString(text).toClojure();
    }
}
