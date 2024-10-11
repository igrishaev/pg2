package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Int4 extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Short s) {
            return BBTool.ofInt(s.intValue());
        } else if (x instanceof Integer i) {
            return BBTool.ofInt(i);
        } else if (x instanceof Long l) {
            return BBTool.ofInt(l.intValue());
        } else {
            return binEncodingError(x);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return x.toString();
    }

    @Override
    public Integer decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return bb.getInt();
    }

    @Override
    public Integer decodeTxt(final String text, final CodecParams codecParams) {
        return Integer.parseInt(text);
    }
}
