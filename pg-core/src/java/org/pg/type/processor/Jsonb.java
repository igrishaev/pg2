package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.codec.JsonBin;
import org.pg.codec.JsonTxt;

import java.nio.ByteBuffer;

public class Jsonb extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        return JsonBin.encodeJSONB(x, codecParams);
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return JsonTxt.encodeJson(x, codecParams);
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return JsonBin.decodeJSONB(bb, codecParams);
    }

    @Override
    public Object decodeTxt(final String string, final CodecParams codecParams) {
        return JsonTxt.decodeJson(string, codecParams);
    }
}
