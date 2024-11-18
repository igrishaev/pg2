package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class Box extends AProcessor {

    public static final int oid = OID.BOX;

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof org.pg.type.Box b) {
            return b.toByteBuffer();
        } else if (x instanceof String s) {
            return org.pg.type.Box.fromString(s).toByteBuffer();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Box.fromMap(m).toByteBuffer();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Box.fromList(l).toByteBuffer();
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof org.pg.type.Box b) {
            return b.toString();
        } else if (x instanceof String s) {
            return org.pg.type.Box.fromString(s).toString();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Box.fromMap(m).toString();
        } else if (x instanceof List<?> l) {
            return org.pg.type.Box.fromList(l).toString();
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public org.pg.type.Box decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return org.pg.type.Box.fromByteBuffer(bb);
    }

    @Override
    public org.pg.type.Box decodeTxt(final String text, final CodecParams codecParams) {
        return org.pg.type.Box.fromString(text);
    }
}
