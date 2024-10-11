package org.pg.type.processor;

import org.pg.Const;
import org.pg.codec.CodecParams;
import org.pg.json.JSON;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class Json extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Const.JSON_ENC_BUF_SIZE);
        JSON.writeValue(codecParams.objectMapper(), out, x);
        final byte[] bytes = out.toByteArray();
        return ByteBuffer.wrap(bytes);
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return JSON.writeValueToString(codecParams.objectMapper(), x);
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return JSON.readValue(codecParams.objectMapper(), bb);
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        return JSON.readValue(codecParams.objectMapper(), text);
    }
}
