package org.pg.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pg.Const;
import org.pg.error.PGError;
import org.pg.json.JSON;
import org.pg.processor.Array;
import org.pg.processor.IProcessor;
import org.pg.processor.Processors;
import org.pg.type.PGType;
import org.pg.util.SQLTool;

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
    private final Map<Integer, IProcessor> oidMap = new HashMap<>();
    private final Map<String, Integer> oidCache = new HashMap<>();

    @Override
    public String toString() {
        return String.format(
                "CodecParams[clientCharset=%s, " +
                        "serverCharset=%s, timeZone=%s, dateStyle=%s, " +
                        "integerDatetime=%s, objectMapper=%s, " +
                        "oidMap=%s]",
                clientCharset,
                serverCharset,
                timeZone,
                dateStyle,
                integerDatetime,
                objectMapper,
                oidMap
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

    public void processTypeMap(final Map<Object, IProcessor> typeMap) {
        String fullName;
        IProcessor processor;
        Integer oid;
        for (Map.Entry<Object, IProcessor> me: typeMap.entrySet()) {
            fullName = SQLTool.fullTypeName(me.getKey());
            processor = me.getValue();
            oid = oidCache.get(fullName);
            if (oid == null) {
                throw new PGError("unknown postgres type: %s", fullName);
            }
            oidMap.put(oid, processor);
        }
    }

    public Integer getOidByType(final String fullType) {
        return oidCache.get(fullType);
    }

    public void setPgType(final PGType pgType) {
        oidCache.put(pgType.fullName(), pgType.oid());

        final int oid = pgType.oid();
        final String signature = pgType.signature();

        if (pgType.isElem()) {
            final int oidArr = pgType.typarray();
            oidMap.put(oidArr, new Array(oidArr, pgType.oid()));
        }

        if (pgType.isEnum()) {
            oidMap.put(oid, Processors.defaultEnum);
        } else if (signature.equals(Const.TYPE_SIG_VECTOR)) {
            oidMap.put(oid, Processors.vector);
        } else if (signature.equals(Const.TYPE_SIG_SPARSEVEC)) {
            oidMap.put(oid, Processors.sparsevec);
        } else if (pgType.isArray()) {
            oidMap.put(oid, new Array(oid, pgType.typelem()));
        } else {
            oidMap.put(oid, Processors.unsupported);
        }
    }

    @SuppressWarnings("unused")
    public boolean isKnownOid(final int oid) {
        return Processors.isKnownOid(oid) || oidMap.containsKey(oid);
    }

    public IProcessor getProcessor(final int oid) {
        // get from a global defaults
        IProcessor processor = Processors.getProcessor(oid);
        if (processor != null) {
            return processor;
        }
        // local session overrides
        processor = oidMap.get(oid);
        if (processor != null) {
            return processor;
        }
        // give up
        return Processors.unsupported;
    }
}
