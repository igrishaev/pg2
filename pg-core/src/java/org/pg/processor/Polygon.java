package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.util.List;

public class Polygon extends AProcessor {

    public static final int oid = OID.POLYGON;

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof org.pg.type.Polygon p) {
            return p.toByteBuffer();
        } else if (x instanceof String s) {
            return org.pg.type.Polygon.fromString(s).toByteBuffer();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Polygon.fromList(l).toByteBuffer();
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof org.pg.type.Polygon p) {
            return p.toString();
        } else if (x instanceof String s) {
            return org.pg.type.Polygon.fromString(s).toSQL();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Polygon.fromList(l).toSQL();
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return org.pg.type.Polygon.fromByteBuffer(bb).toClojure();
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        return org.pg.type.Polygon.fromString(text).toClojure();
    }
}
