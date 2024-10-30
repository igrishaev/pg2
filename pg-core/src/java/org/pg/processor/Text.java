package org.pg.processor;

import clojure.lang.Symbol;
import org.pg.codec.CodecParams;
import org.pg.codec.PrimitiveBin;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.util.UUID;

public class Text extends AProcessor {

    public final int oid;

    public Text(final int oid) {
        this.oid = oid;
    }

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return PrimitiveBin.encodeString(s, codecParams);
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else if (x instanceof UUID u) {
            return u.toString();
        } else if (x instanceof Symbol s) {
            return s.toString();
        } else if (x instanceof Character c) {
            return c.toString();
        } else {
            return txtEncodingError(x, oid);
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
