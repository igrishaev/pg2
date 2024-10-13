package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.codec.PrimitiveBin;
import org.pg.error.PGError;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Text extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return PrimitiveBin.encodeString(s, codecParams);
        } else {
            throw new PGError("value %s must be a string", x);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else {
            throw new PGError("value %s must be a string", x);
        }
    }

    @Override
    public String decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return BBTool.getRestString(bb, codecParams.serverCharset());
    }

    @Override
    public String decodeTxt(final String text, final CodecParams codecParams) {
        return text;
    }
}
