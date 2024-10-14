package org.pg.codec;

import org.pg.error.PGError;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class PrimitiveBin {


    // TODO: remove this
    public static ByteBuffer encodeBool(final boolean b) {
        return BBTool.ofBool(b);
    }

    public static boolean decodeBool(final ByteBuffer bb) {
        final byte b = bb.get();
        return switch (b) {
            case 0: yield false;
            case 1: yield true;
            default: throw new PGError("incorrect binary boolean value: %s", b);
        };
    }

    public static ByteBuffer encodeShort(final short s) {
        return BBTool.ofShort(s);
    }

    public static ByteBuffer encodeString(final String string, final CodecParams codecParams) {
        final byte[] bytes = string.getBytes(codecParams.clientCharset());
        return ByteBuffer.wrap(bytes);
    }

}
