package org.pg.msg;

import clojure.lang.*;
import org.pg.util.IClojure;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

public record NoticeResponse(Map<String, String> fields) implements IClojure {

    public Associative toClojure () {
        Associative map = PersistentHashMap.EMPTY;
        for (Map.Entry<String, String> e: fields.entrySet()) {
            map = RT.assoc(map, Keyword.intern(e.getKey()), e.getValue());
        }
        return map.assoc(
                Keyword.intern("msg"),
                Keyword.intern("NoticeResponse")
        );
    }

    public static NoticeResponse fromByteBuffer (final ByteBuffer buf, final Charset charset) {
        final Map<String, String> fields = FieldParser.parseFields(buf, charset);
        return new NoticeResponse(fields);
    }
}
