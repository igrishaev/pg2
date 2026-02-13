package org.pg.processor;

import org.pg.codec.CodecParams;

import java.nio.ByteBuffer;

public class Void extends AProcessor {

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return null;
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        return null;
    }

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        return ByteBuffer.allocate(0);
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return "";
    }
}