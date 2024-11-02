package org.pg.processor;

import org.pg.codec.CodecParams;

import java.nio.ByteBuffer;

public interface IProcessor {
    ByteBuffer encodeBin(final Object value, final CodecParams codecParams);
    String encodeTxt(final Object value, final CodecParams codecParams);
    Object decodeBin(final ByteBuffer bb, final CodecParams codecParams);
    Object decodeTxt(final String text, final CodecParams codecParams);
}
