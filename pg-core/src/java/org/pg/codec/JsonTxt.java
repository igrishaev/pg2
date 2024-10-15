package org.pg.codec;

import org.pg.json.JSON;

public class JsonTxt {

    public static String encodeJson(final Object x, final CodecParams codecParams) {
        if (x instanceof JSON.Wrapper jw) {
            return encodeJson(jw.value(), codecParams);
        } else if (x instanceof String s) {
            return s;
        } else {
            return JSON.writeValueToString(codecParams.objectMapper(), x);
        }
    }

    public static Object decodeJson(final String string, final CodecParams codecParams) {
        return JSON.readValue(codecParams.objectMapper(), string);
    }

}
