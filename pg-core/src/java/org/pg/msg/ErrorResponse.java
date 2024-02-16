package org.pg.msg;

import clojure.lang.IPersistentCollection;
import clojure.lang.ITransientAssociative;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import org.pg.proto.IClojure;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

public record ErrorResponse (Map<String, String> fields) implements IClojure {

    public static ErrorResponse fromByteBuffer(final ByteBuffer buf, final Charset charset) {
        final Map<String, String> fields = FieldParser.parseFields(buf, charset);
        return new ErrorResponse(fields);
    }

    @Override
    public IPersistentCollection toClojure() {
        ITransientAssociative map = PersistentHashMap.EMPTY.asTransient();
        for (final Map.Entry<String, String> entry: fields.entrySet()) {
            map = map.assoc(Keyword.intern(entry.getKey()), entry.getValue());
        }
        return map.persistent();
    }
}
