package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class UUID extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof java.util.UUID u) {
            return BBTool.ofUUID(u);
        } else if (x instanceof String s) {
            return BBTool.ofUUID(java.util.UUID.fromString(s));
        } else {
            return binEncodingError(x);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof java.util.UUID u) {
            return u.toString();
        } else if (x instanceof String s) {
            return s;
        } else {
            return txtEncodingError(x);
        }
    }

    @Override
    public java.util.UUID decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        final long high = bb.getLong();
        final long low = bb.getLong();
        return new java.util.UUID(high, low);
    }

    @Override
    public java.util.UUID decodeTxt(final String text, final CodecParams codecParams) {
        return java.util.UUID.fromString(text);
    }
}
