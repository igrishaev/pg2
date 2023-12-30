package org.pg.type;

import clojure.lang.Keyword;
import clojure.lang.PersistentVector;
import clojure.lang.PersistentHashMap;
import clojure.lang.Ratio;
import clojure.lang.Symbol;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.pg.PGError;
import jsonista.jackson.PersistentVectorDeserializer;
import jsonista.jackson.PersistentHashMapDeserializer;
import jsonista.jackson.KeywordKeyDeserializer;
import jsonista.jackson.KeywordSerializer;
import jsonista.jackson.RatioSerializer;
import jsonista.jackson.SymbolSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


public class JSON {

    public record Wrapper (Object value) {}

    @SuppressWarnings("unused")
    public static Wrapper wrap (final Object value) {
        return new Wrapper(value);
    }

    static final ObjectMapper mapper = new ObjectMapper();

    static {
        final SimpleModule module = new SimpleModule("pg");
        module.addDeserializer(List.class, new PersistentVectorDeserializer());
        module.addDeserializer(Map.class, new PersistentHashMapDeserializer());
        module.addSerializer(Keyword.class, new KeywordSerializer(false));
        module.addKeySerializer(Keyword.class, new KeywordSerializer(true));
        module.addKeyDeserializer(Object.class, new KeywordKeyDeserializer());
        module.addSerializer(Ratio.class, new RatioSerializer());
        module.addSerializer(Symbol.class, new SymbolSerializer());
        module.addKeyDeserializer(Object.class, new KeywordKeyDeserializer());
        mapper.registerModule(module);
    }

    static Object decodeError(final Throwable e) {
        throw new PGError(e, "JSON decode error");
    }

    static void encodeError(final Throwable e, final Object value) {
        throw new PGError(e, "JSON encode error, value: %s", value);
    }

    public static Object readValue (final String input) {
        try {
            return mapper.readValue(input, Object.class);
        } catch (JsonProcessingException e) {
            return decodeError(e);
        }
    }

    public static Object readValueBinary (final ByteBuffer buf) {
        final byte b = buf.get();
        if (b == 1) {
            buf.limit(buf.limit() - 1);
        }
        else {
            buf.position(buf.position() - 1);
        }
        return readValue(buf);
    }

    public static Object readValue (final ByteBuffer buf) {
        final int offset = buf.arrayOffset() + buf.position();
        final int len = buf.limit();
        try {
            return mapper.readValue(buf.array(), offset, len, Object.class);
        } catch (IOException e) {
            return decodeError(e);
        }
    }

    public static void writeValue (final OutputStream outputStream, final Object value) {
        try {
            mapper.writeValue(outputStream, value);
        } catch (IOException e) {
            encodeError(e, value);
        }
    }

    public static void writeValue (final Writer writer, final Object value) {
        try {
            mapper.writeValue(writer, value);
        } catch (IOException e) {
            encodeError(e, value);
        }
    }

    @SuppressWarnings("unused")
    public static String writeValueToString (final Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            encodeError(e, value);
            return "";
        }
    }

    public static void main (final String[] args) {
        final ByteBuffer buf = ByteBuffer.wrap("[1, 2, 3]".getBytes());
        System.out.println(readValue(buf));

        final PersistentVector vector = PersistentVector.create(
                1,
                Keyword.intern("foo", "bar"),
                Keyword.intern("no-namespace"),
                true,
                null,
                PersistentVector.create("nested", 42),
                PersistentHashMap.create(
                        "key1", 42,
                        Keyword.intern("aaa", "bbb"), 100,
                        Keyword.intern("aaa"), Symbol.intern("foo", "bar")
                )
        );

        // System.out.println(vector);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeValue(out, vector);
        System.out.println(out.toString(StandardCharsets.UTF_8));
        System.out.println(readValue(out.toString(StandardCharsets.UTF_8)));

    }



}
