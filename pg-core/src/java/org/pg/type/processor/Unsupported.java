package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.error.PGError;
import org.pg.util.BBTool;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

public class Unsupported extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return ByteBuffer.wrap(s.getBytes(codecParams.clientCharset()));
        } else if (x instanceof ByteBuffer bb) {
            return bb;
        } else if (x instanceof byte[] ba) {
            return ByteBuffer.wrap(ba);
        } else {
            throw new PGError(
                    "cannot binary-encode value %s, type %s. " +
                            "Try to pass its SQL binary representation as a byte array or a byte buffer.",
                    x, x.getClass().getName()
            );
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else {
            throw new PGError(
                    "cannot text-encode value %s, type %s. " +
                            "Try to pass its SQL text representation as a string.",
                    x, x.getClass().getName()
            );
        }
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return BBTool.getRestBytes(bb);
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        return text;
    }
}
