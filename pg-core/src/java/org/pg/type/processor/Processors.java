package org.pg.type.processor;

import org.pg.enums.OID;

import java.util.HashMap;
import java.util.Map;

public class Processors {

    public static IProcessor defaultProcessor = new Default();

    @SuppressWarnings("unused")
    public static IProcessor defaultEnum = new Enum();

    static Map<Integer, IProcessor> oidMap = new HashMap<>();
    static {

        // numbers
        final IProcessor int4 = new Int4();
        oidMap.put(OID.INT2, new Int2());
        oidMap.put(OID.INT4, int4);
        oidMap.put(OID.OID, int4);
        oidMap.put(OID.INT8, new Int8());
        oidMap.put(OID.NUMERIC, new Numeric());

        // text
        final IProcessor text = new Text();
        oidMap.put(OID.VARCHAR, text);
        oidMap.put(OID.TEXT,    text);
        oidMap.put(OID.NAME,    text);
        oidMap.put(OID.BPCHAR,  text);

        // misc
        oidMap.put(OID.UUID, new UUID());
        oidMap.put(OID.JSON, new Json());
        oidMap.put(OID.JSONB, new Jsonb());

        // date & time
        oidMap.put(OID.TIMESTAMPTZ, new Timestamptz());
        oidMap.put(OID.TIMESTAMP, new Timestamp());
        oidMap.put(OID.DATE, new Date());
        oidMap.put(OID.TIME, new Time());

    }

    public static IProcessor getProcessor(final int oid) {
        return oidMap.get(oid);
    }

}
