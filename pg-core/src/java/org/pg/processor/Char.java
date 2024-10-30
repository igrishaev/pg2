package org.pg.processor;

import org.pg.Const;
import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.error.PGError;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Char extends AProcessor {

    public static final int oid = OID.CHAR;

    private static ByteBuffer stringToBytes(final String x, final CodecParams codecParams) {
        final byte[] bytes = x.getBytes(codecParams.clientCharset());
        return ByteBuffer.wrap(bytes);
    }

    private static String fromString(final String string) {
        if (string.length() == 1) {
            return string;
        } {
            throw new PGError(
                    "String %s must be exactly of one character " +
                            "because the database type is CHAR", string
            );
        }
    }

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Character c) {
            return stringToBytes(c.toString(), codecParams);
        } else if (x instanceof String s) {
            return stringToBytes(fromString(s), codecParams);
        }
        else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Character c) {
            return c.toString();
        } else if (x instanceof String s) {
            return fromString(s);
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public Character decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        final String string = BBTool.getRestString(bb, codecParams.serverCharset());
        if (string.isEmpty()) {
            return Const.NULL_CHAR;
        } else {
            return string.charAt(0);
        }
    }

    @Override
    public Character decodeTxt(final String string, final CodecParams codecParams) {
        if (string.isEmpty()) {
            return Const.NULL_CHAR;
        } else {
            return string.charAt(0);
        }
    }
}
