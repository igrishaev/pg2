package org.pg.type.processor;

import org.pg.Const;
import org.pg.codec.CodecParams;
import org.pg.json.JSON;
import org.pg.util.IOTool;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class Jsonb extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Const.JSON_ENC_BUF_SIZE);
        out.write(Const.JSONB_VERSION);
        if (x instanceof String s) {
            final byte[] buf = s.getBytes(codecParams.clientCharset());
            IOTool.write(out, buf);
        } else {
            JSON.writeValue(codecParams.objectMapper(), out, x);
        }
        final byte[] bytes = out.toByteArray();
        return ByteBuffer.wrap(bytes);
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else {
            return JSON.writeValueToString(codecParams.objectMapper(), x);
        }
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        bb.get(); // skip version
        return JSON.readValue(codecParams.objectMapper(), bb);
    }

    @Override
    public Object decodeTxt(final String text, final CodecParams codecParams) {
        return JSON.readValue(codecParams.objectMapper(), text);
    }
}
