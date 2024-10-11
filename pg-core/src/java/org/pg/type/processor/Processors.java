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
        oidMap.put(OID.INT2, new Int2());
        oidMap.put(OID.INT4, new Int4());
        oidMap.put(OID.OID,  new Int4());
        oidMap.put(OID.INT8, new Int8());

        oidMap.put(OID.VARCHAR, new Text());
        oidMap.put(OID.TEXT,    new Text());

        oidMap.put(OID.UUID, new UUID());
        oidMap.put(OID.JSON, new Json());
    }

    public static IProcessor getProcessor(final int oid) {
        return oidMap.get(oid);
    }

}
