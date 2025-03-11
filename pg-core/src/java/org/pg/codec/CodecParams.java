package org.pg.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pg.Const;
import org.pg.json.JSON;
import org.pg.processor.IProcessor;
import org.pg.processor.Processors;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class CodecParams {

    private Charset clientCharset = Const.clientCharset;
    private Charset serverCharset = Const.serverCharset;
    private ZoneId timeZone = Const.timeZone;
    private String dateStyle = Const.dateStyle;
    private boolean integerDatetime = Const.integerDatetime;
    private ObjectMapper objectMapper = JSON.defaultMapper;
    private final Map<Integer, IProcessor> oidProcessorMap = new HashMap<>();

    @Override
    public String toString() {
        return String.format("CodecParams[clientCharset=%s, serverCharset=%s, timeZone=%s, dateStyle=%s, integerDatetime=%s, objectMapper=%s, oidMap=%s]",
                clientCharset,
                serverCharset,
                timeZone,
                dateStyle,
                integerDatetime,
                objectMapper,
                oidProcessorMap
        );
    }

    public static CodecParams create() {
        return new CodecParams();
    }

    public Charset clientCharset() {
        return clientCharset;
    }

    @SuppressWarnings("UnusedReturnValue")
    public CodecParams clientCharset(final Charset clientCharset) {
        this.clientCharset = clientCharset;
        return this;
    }

    public Charset serverCharset() {
        return serverCharset;
    }

    @SuppressWarnings("UnusedReturnValue")
    public CodecParams serverCharset(final Charset serverCharset) {
        this.serverCharset = serverCharset;
        return this;
    }

    @SuppressWarnings("unused")
    public ZoneId timeZone() {
        return timeZone;
    }

    @SuppressWarnings("UnusedReturnValue")
    public CodecParams timeZone(final ZoneId timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    @SuppressWarnings("unused")
    public String dateStyle() {
        return dateStyle;
    }

    @SuppressWarnings("UnusedReturnValue")
    public CodecParams dateStyle(final String dateStyle) {
        this.dateStyle = dateStyle;
        return this;
    }

    @SuppressWarnings("unused")
    public boolean integerDatetime() {
        return integerDatetime;
    }

    @SuppressWarnings("UnusedReturnValue")
    public CodecParams integerDatetime(final boolean integerDatetime) {
        this.integerDatetime = integerDatetime;
        return this;
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    @SuppressWarnings("UnusedReturnValue")
    public CodecParams objectMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public void setProcessor(final int oid, final IProcessor processor) {
        oidProcessorMap.put(oid, processor);
    }

    public IProcessor getProcessor(final int oid) {
        IProcessor typeProcessor = Processors.getProcessor(oid);
        if (typeProcessor == null) {
            typeProcessor = oidProcessorMap.get(oid);
        }
        if (typeProcessor == null) {
            typeProcessor = Processors.unsupported;
        }
        return typeProcessor;
    }

}
