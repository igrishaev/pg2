package org.pg.type.processor;

import org.pg.Const;
import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Char extends AProcessor {

    public static final int oid = OID.CHAR;

    private static ByteBuffer stringToBytes(final String x, final CodecParams codecParams) {
        final byte[] bytes = x.getBytes(codecParams.clientCharset());
        return ByteBuffer.wrap(bytes);
    }

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return stringToBytes(s, codecParams);
        } else if (x instanceof Character c) {
            return stringToBytes(c.toString(), codecParams);
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else if (x instanceof Character c) {
            return c.toString();
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
