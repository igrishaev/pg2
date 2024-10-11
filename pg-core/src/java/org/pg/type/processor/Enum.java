package org.pg.type.processor;

import clojure.lang.Keyword;
import clojure.lang.Symbol;
import org.pg.codec.CodecParams;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Enum extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return BBTool.ofString(s, codecParams.clientCharset());
        } else if (x instanceof Keyword kw) {
            return BBTool.ofString(kw.getName(), codecParams.clientCharset());
        } else if (x instanceof Symbol s) {
            return BBTool.ofString(s.toString(), codecParams.clientCharset());
        } else {
            return binEncodingError(x);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else if (x instanceof Keyword kw) {
            return kw.getName();
        } else if (x instanceof Symbol s) {
            return s.toString();
        } else {
            return txtEncodingError(x);
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
