package org.pg.codec;

import org.pg.Const;
import org.pg.json.JSON;
import org.pg.util.IOTool;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class JsonBin {

    public static ByteBuffer encodeJSONB (final Object x, final CodecParams codecParams) {
        if (x instanceof JSON.Wrapper jw) {
            return encodeJSONB(jw.value(), codecParams);
        }
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

    public static ByteBuffer encodeJSON (final Object x, final CodecParams codecParams) {
        if (x instanceof JSON.Wrapper jw) {
            return encodeJSON(jw.value(), codecParams);
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Const.JSON_ENC_BUF_SIZE);
        if (x instanceof String s) {
            final byte[] buf = s.getBytes(codecParams.clientCharset());
            IOTool.write(out, buf);
        } else {
            JSON.writeValue(codecParams.objectMapper(), out, x);
        }
        final byte[] bytes = out.toByteArray();
        return ByteBuffer.wrap(bytes);
    }

    public static Object decodeJSONB (final ByteBuffer bb, final CodecParams codecParams) {
        bb.get(); // skip version
        return JSON.readValue(codecParams.objectMapper(), bb);
    }

    public static Object decodeJSON (final ByteBuffer bb, final CodecParams codecParams) {
        return JSON.readValue(codecParams.objectMapper(), bb);
    }
}
