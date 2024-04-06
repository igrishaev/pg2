package org.pg.msg.server;

import clojure.lang.*;
import org.pg.clojure.IClojure;
import org.pg.clojure.KW;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

public record NoticeResponse(Map<String, String> fields) implements IClojure, IServerMessage {

    public Associative toClojure () {
        Associative map = PersistentHashMap.EMPTY;
        for (Map.Entry<String, String> e: fields.entrySet()) {
            map = RT.assoc(map, Keyword.intern(e.getKey()), e.getValue());
        }
        return map.assoc(KW.msg, KW.NoticeResponse);
    }

    public static NoticeResponse fromByteBuffer (final ByteBuffer buf, final Charset charset) {
        final Map<String, String> fields = FieldParser.parseFields(buf, charset);
        return new NoticeResponse(fields);
    }
}
