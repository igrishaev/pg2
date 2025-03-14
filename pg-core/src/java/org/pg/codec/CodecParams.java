package org.pg.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pg.Const;
import org.pg.error.PGError;
import org.pg.json.JSON;
import org.pg.processor.IProcessor;
import org.pg.processor.Processors;
import org.pg.type.PGType;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/*
A storage of data encoding and decoding parameters, e.g. the current
client charset, client charset, date style and so on. Most of the
data gets populated by the Connection object during the authorization
pipeline.
 */
public class CodecParams {
    private Charset clientCharset = Const.clientCharset;
    private Charset serverCharset = Const.serverCharset;
    private ZoneId timeZone = Const.timeZone;
    private String dateStyle = Const.dateStyle;
    private boolean integerDatetime = Const.integerDatetime;
    private ObjectMapper objectMapper = JSON.defaultMapper;
    private final Map<Integer, PGType> pgTypes = new HashMap<>(Const.pgTypeMapSize);
    private final Map<String, Integer> typeToOID = new HashMap<>(Const.typeToOIDSize);

    @Override
    public String toString() {
        return String.format("CodecParams[clientCharset=%s, serverCharset=%s, timeZone=%s, dateStyle=%s, integerDatetime=%s, objectMapper=%s, pgTypes=%s]",
                clientCharset,
                serverCharset,
                timeZone,
                dateStyle,
                integerDatetime,
                objectMapper,
                pgTypes
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

    public void setPgType(final PGType pgType) {
        final int oid = pgType.oid();
        final String typeName = pgType.nspname() + "." + pgType.typname();
        pgTypes.put(oid, pgType);
        typeToOID.put(typeName, oid);
    }

    public int typeToOid(final String pgType) {
        final Integer oid = typeToOID.get(pgType);
        if (oid == null) {
            throw new PGError("unknown postgres type: %s", pgType);
        } else {
            return oid;
        }
    }

    public IProcessor getProcessor(final int oid) {
        final IProcessor processor = Processors.getProcessor(oid);
        if (processor != null) {
            return processor;
        }
        final PGType pgType = pgTypes.get(oid);
        if (pgType == null) {
            return Processors.unsupported;
        }
        if (pgType.isVector()) {
            return Processors.vector;
        } else if (pgType.isSparseVector()) {
            return Processors.sparsevec;
        } else if (pgType.isEnum()) {
            return Processors.defaultEnum;
        } else {
            return Processors.unsupported;
        }
    }



}
