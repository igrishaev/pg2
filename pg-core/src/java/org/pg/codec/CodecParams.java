package org.pg.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pg.json.JSON;
import org.pg.type.processor.IProcessor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public record CodecParams (
        Charset clientCharset,
        Charset serverCharset,
        ZoneId timeZone,
        String dateStyle,
        boolean integerDatetime,
        ObjectMapper objectMapper,
        Map<Integer, IProcessor> oidMap
) {

    public void setProcessor(final int oid, final IProcessor processor) {
        oidMap.put(oid, processor);
    }

    public IProcessor getProcessor(final int oid) {
        return oidMap.get(oid);
    }

    public static Builder builder () {
        return new Builder();
    }

    public Builder toBuilder () {
        return new Builder(this);
    }

    public static CodecParams standard() {
        return builder().build();
    }

    public CodecParams withClientCharset(final Charset clientCharset) {
        return this.toBuilder().clientCharset(clientCharset).build();
    }

    public CodecParams withServerCharset(final Charset serverCharset) {
        return this.toBuilder().serverCharset(serverCharset).build();
    }

    public CodecParams withDateStyle(final String dateStyle) {
        return this.toBuilder().dateStyle(dateStyle).build();
    }

    public CodecParams withTimeZoneId(final ZoneId zoneId) {
        return this.toBuilder().timeZone(zoneId).build();
    }

    public CodecParams withIntegerDatetime(final boolean integerDatetime) {
        return this.toBuilder().integerDatetime(integerDatetime).build();
    }

    public static class Builder {

        private Charset clientCharset = StandardCharsets.UTF_8;
        private Charset serverCharset = StandardCharsets.UTF_8;
        private ZoneId timeZone = ZoneOffset.UTC;
        private String dateStyle = "ISO, DMY";
        private boolean integerDatetime = true;
        private ObjectMapper objectMapper = JSON.defaultMapper;
        private final Map<Integer, IProcessor> oidMapper = new HashMap<>();

        public Builder() {}

        public Builder(final CodecParams codecParams) {
            this.clientCharset = codecParams.clientCharset;
            this.serverCharset = codecParams.serverCharset;
            this.timeZone = codecParams.timeZone;
            this.dateStyle = codecParams.dateStyle;
            this.integerDatetime = codecParams.integerDatetime;
            this.objectMapper = codecParams.objectMapper;
        }

        public CodecParams build() {
            return new CodecParams(
                    this.clientCharset,
                    this.serverCharset,
                    this.timeZone,
                    this.dateStyle,
                    this.integerDatetime,
                    this.objectMapper,
                    this.oidMapper
            );
        }

        public Builder clientCharset (final Charset clientCharset) {
            this.clientCharset = clientCharset;
            return this;
        }

        public Builder serverCharset (final Charset serverCharset) {
            this.serverCharset = serverCharset;
            return this;
        }

        public Builder timeZone (final ZoneId timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder dateStyle (final String dateStyle) {
            this.dateStyle = dateStyle;
            return this;
        }

        public Builder integerDatetime (final boolean integerDatetime) {
            this.integerDatetime = integerDatetime;
            return this;
        }

        public Builder objectMapper (final ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

    }


}
