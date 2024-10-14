package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.error.PGError;

import java.nio.ByteBuffer;

public abstract class AProcessor implements IProcessor {

    public static String txtEncodingError(final Object x) {
        if (x == null) {
            throw new PGError("cannot text-encode a null value");
        }
        throw new PGError(
                "cannot text-encode, value: %s, type: %s",
                x, x.getClass().getName()
        );
    }

    public static String txtEncodingError(final Object x, final int oid) {
        if (x == null) {
            throw new PGError("cannot text-encode a null value");
        }
        throw new PGError(
                "cannot text-encode, oid: %s, value: %s, type: %s",
                oid, x, x.getClass().getName()
        );
    }

    public static ByteBuffer binEncodingError(final Object x) {
        if (x == null) {
            throw new PGError("cannot binary-encode a null value");
        }
        throw new PGError(
                "cannot binary-encode, value: %s, type: %s",
                x, x.getClass().getCanonicalName()
        );
    }

    public static ByteBuffer binEncodingError(final Object x, final int oid) {
        if (x == null) {
            throw new PGError("cannot binary-encode a null value");
        }
        throw new PGError(
                "cannot binary-encode, oid: %s, value: %s, type: %s",
                oid, x, x.getClass().getCanonicalName()
        );
    }

    @Override
    public ByteBuffer encodeBin(final Object value, final CodecParams codecParams) {
        throw new PGError("method 'encodeBin' is not implemented, type: %s", value.getClass().getName());
    }

    @Override
    public String encodeTxt(final Object value, final CodecParams codecParams) {
        throw new PGError("method 'encodeTxt' is not implemented, type: %s", value.getClass().getName());
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
