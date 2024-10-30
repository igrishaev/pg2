package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.error.PGError;
import org.pg.util.TypeTool;

import java.nio.ByteBuffer;

public abstract class AProcessor implements IProcessor {

    public static String txtEncodingError(final Object x) {
        throw new PGError("cannot text-encode: %s", TypeTool.repr(x));
    }

    public static String txtEncodingError(final Object x, final int oid) {
        throw new PGError("cannot text-encode, oid: %s, %s", oid, TypeTool.repr(x));
    }

    public static ByteBuffer binEncodingError(final Object x) {
        throw new PGError("cannot binary-encode: %s", TypeTool.repr(x));
    }

    public static ByteBuffer binEncodingError(final Object x, final int oid) {
        throw new PGError("cannot binary-encode, oid: %s, %s", oid, TypeTool.repr(x));
    }

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        throw new PGError("method 'encodeBin' is not implemented: %s", TypeTool.repr(x));
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        throw new PGError("method 'encodeTxt' is not implemented: %s", TypeTool.repr(x));
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        throw new PGError("method 'decodeBin' is not implemented");
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        throw new PGError("method 'decodeTxt' is not implemented");
    }
}
