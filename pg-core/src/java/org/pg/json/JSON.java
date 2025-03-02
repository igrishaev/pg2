package org.pg.json;

import clojure.lang.IDeref;
import clojure.lang.Keyword;
import clojure.lang.PersistentVector;
import clojure.lang.PersistentHashMap;
import clojure.lang.Ratio;
import clojure.lang.Symbol;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.pg.error.PGError;
import jsonista.jackson.PersistentVectorDeserializer;
import jsonista.jackson.PersistentHashMapDeserializer;
import jsonista.jackson.KeywordKeyDeserializer;
import jsonista.jackson.KeywordSerializer;
import jsonista.jackson.RatioSerializer;
import jsonista.jackson.SymbolSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;


public final class JSON {

    public record Wrapper (Object value) implements IDeref {
        @Override
        public Object deref() {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static Wrapper wrap (final Object value) {
        if (value instanceof Wrapper w) {
            return w;
        } else {
            return new Wrapper(value);
        }
    }

    public static final ObjectMapper defaultMapper = new ObjectMapper();

    static {
        final SimpleModule module = new SimpleModule("pg");
        module.addDeserializer(List.class, new PersistentVectorDeserializer());
        module.addDeserializer(Map.class, new PersistentHashMapDeserializer());
        module.addSerializer(Keyword.class, new KeywordSerializer(false));
        module.addKeySerializer(Keyword.class, new KeywordSerializer(true));
        module.addSerializer(Temporal.class, new TemporalSerializer());
        module.addKeyDeserializer(Object.class, new KeywordKeyDeserializer());
        module.addSerializer(Ratio.class, new RatioSerializer());
        module.addSerializer(Symbol.class, new SymbolSerializer());
        module.addKeyDeserializer(Object.class, new KeywordKeyDeserializer());
        defaultMapper.registerModule(module);
    }

    static Object decodeError(final Throwable e) {
        throw new PGError(e, "JSON decode error");
    }

    static void encodeError(final Throwable e, final Object value) {
        throw new PGError(e, "JSON encode error, value: %s", value);
    }

    public static Object readValue (final ObjectMapper objectMapper, final String input) {
        try {
            return objectMapper.readValue(input, Object.class);
        }
        catch (final IOException e) {
            return decodeError(e);
        }
    }

    @SuppressWarnings("unused")
    public static Object readValue (final String input) {
        return readValue(defaultMapper, input);
    }

    @SuppressWarnings("unused")
    public static Object readValue (final ObjectMapper objectMapper, final InputStream inputStream) {
        try {
            return objectMapper.readValue(inputStream, Object.class);
        }
        catch (final IOException e) {
            return decodeError(e);
        }
    }

    @SuppressWarnings("unused")
    public static Object readValue (final InputStream inputStream) {
        return readValue(defaultMapper, inputStream);
    }

    @SuppressWarnings("unused")
    public static Object readValue (final ObjectMapper objectMapper, final Reader reader) {
        try {
            return objectMapper.readValue(reader, Object.class);
        }
        catch (final IOException e) {
            return decodeError(e);
        }
    }

    @SuppressWarnings("unused")
    public static Object readValue (final Reader reader) {
        return readValue(defaultMapper, reader);
    }

    public static Object readValue (final ByteBuffer buf) {
        return readValue(defaultMapper, buf);
    }

    public static Object readValue (final ObjectMapper objectMapper, final ByteBuffer buf) {
        final int offset = buf.arrayOffset() + buf.position();
        final int len = buf.limit() - offset;
        try {
            return objectMapper.readValue(buf.array(), offset, len, Object.class);
        } catch (final IOException e) {
            return decodeError(e);
        }
    }

    public static void writeValue (final ObjectMapper objectMapper, final OutputStream outputStream, final Object value) {
        try {
            objectMapper.writeValue(outputStream, value);
        } catch (final IOException e) {
            encodeError(e, value);
        }
    }

    public static void writeValue (final OutputStream outputStream, final Object value) {
        writeValue(defaultMapper, outputStream, value);
    }

    public static void writeValue (final ObjectMapper objectMapper, final Writer writer, final Object value) {
        try {
            objectMapper.writeValue(writer, value);
        } catch (final IOException e) {
            encodeError(e, value);
        }
    }

    @SuppressWarnings("unused")
    public static void writeValue (final Writer writer, final Object value) {
        writeValue(defaultMapper, writer, value);
    }

    @SuppressWarnings("unused")
    public static String writeValueToString (final ObjectMapper objectMapper, final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (final JsonProcessingException e) {
            encodeError(e, value);
            return ""; // mute "might be null" note
        }
    }

    @SuppressWarnings("unused")
    public static String writeValueToString (final Object value) {
        return writeValueToString(defaultMapper, value);
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
        // System.out.println(readValue(out.toString(StandardCharsets.UTF_8)));

    }



}
