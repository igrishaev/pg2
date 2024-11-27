package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class Point extends AProcessor {

    public static final int oid = OID.POINT;

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof org.pg.type.Point p) {
            return p.toByteBuffer();
        } else if (x instanceof String s) {
            return org.pg.type.Point.fromSQL(s).toByteBuffer();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Point.fromMap(m).toByteBuffer();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Point.fromList(l).toByteBuffer();
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof org.pg.type.Point p) {
            return p.toSQL();
        } else if (x instanceof String s) {
            return org.pg.type.Point.fromSQL(s).toSQL();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Point.fromMap(m).toSQL();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Point.fromList(l).toSQL();
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return org.pg.type.Point.fromByteBuffer(bb).toClojure();
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        return org.pg.type.Point.fromSQL(text).toClojure();
    }
}
