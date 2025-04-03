package org.pg.codec;

import clojure.lang.Named;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pg.Const;
import org.pg.error.PGError;
import org.pg.json.JSON;
import org.pg.processor.Array;
import org.pg.processor.IProcessor;
import org.pg.processor.Processors;
import org.pg.type.PGType;
import org.pg.util.TypeTool;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    private final Map<Integer, PGType> oidToPGType = new HashMap<>();
    private final Map<String, Integer> typeNameToOid = new HashMap<>();
    private final Map<Integer, IProcessor> oidToProcessor = new HashMap<>();

    @Override
    public String toString() {
        return String.format(
                "CodecParams[clientCharset=%s, " +
                        "serverCharset=%s, timeZone=%s, dateStyle=%s, " +
                        "integerDatetime=%s, objectMapper=%s, " +
                        "oidToPGType=%s, typeToOID=%s, oidToProcessor=%s]",
                clientCharset,
                serverCharset,
                timeZone,
                dateStyle,
                integerDatetime,
                objectMapper,
                oidToPGType,
                typeNameToOid,
                oidToProcessor
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

    public static String coerceStringType(final String type) {
        final String[] parts = type.split("\\.", 2);
        if (parts.length == 1) {
            return Const.defaultSchema + "." + type;
        } else {
            return type;
        }
    }

    public static String objectToPGType(final Object obj) {
        String namespace;
        if (obj instanceof String s) {
            return coerceStringType(s);
        } else if (obj instanceof Named nm) {
            namespace = nm.getNamespace();
            if (namespace == null) {
                namespace = Const.defaultSchema;
            }
            return namespace + "." + nm.getName();
        } else {
            throw new PGError("wrong postgres type: %s", TypeTool.repr(obj));
        }
    }

    public void setProcessor(final Object pgType, final IProcessor processor) {
        final String type = objectToPGType(pgType);
        final Integer oid = typeNameToOid.get(type);
        if (oid == null) {
            throw new PGError("unknown type: %s", TypeTool.repr(pgType));
        }
        oidToProcessor.put(oid, processor);
    }

    public PGType getPgType(final String fullName) {
        final Integer oid = typeNameToOid.get(fullName);
        if (oid == null) return null;
        return oidToPGType.get(oid);
    }

    public void setPgType(final PGType pgType) {
        final int oid = pgType.oid();
        final String fullName = pgType.fullName();
        oidToPGType.put(oid, pgType);
        typeNameToOid.put(fullName, oid);
    }

    public int typeToOid(final String pgType) {
        final Integer oid = typeNameToOid.get(pgType);
        if (oid == null) {
            throw new PGError("unknown postgres type: %s", pgType);
        } else {
            return oid;
        }
    }

    public Collection<PGType> getPgTypes() {
        return oidToPGType.values();
    }

    public IProcessor getProcessor(final int oid) {
        // get from a global map with default types
        IProcessor processor = Processors.getProcessor(oid);
        if (processor != null) {
            return processor;
        }

        // get from overrides
        processor = oidToProcessor.get(oid);
        if (processor != null) {
            return processor;
        }

        // try to guess by fields from pg_type table
        final PGType pgType = oidToPGType.get(oid);
        if (pgType == null) {
            return Processors.unsupported;
        }

        if (pgType.isEnum()) {
            return Processors.defaultEnum;
        }

        if (pgType.isArray()) {
            return new Array(pgType.oid(), pgType.typelem());
        }

        processor = Processors.getCustomProcessor(pgType);
        if (processor == null) {
            return Processors.unsupported;
        }

        return processor;
    }
}
