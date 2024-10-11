package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.error.PGError;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Default extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof byte[] ba) {
            return ByteBuffer.wrap(ba);
        } else if (x instanceof ByteBuffer bb) {
            return bb;
        } else {
            throw new PGError(
                    "don't know how to binary-encode value %s. " +
                    "Try to pass its SQL binary representation as a byte array or a byte buffer",
                    x
            );
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else {
            throw new PGError(
                    "don't know how to text-encode value %s. " +
                    "Try to pass its SQL string representation",
                    x
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
