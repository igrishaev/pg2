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
        oidMap.put(OID.FLOAT4, new Float4());
        oidMap.put(OID.FLOAT8, new Float8());

        // text
        final IProcessor text = new Text();
        oidMap.put(OID.VARCHAR, text);
        oidMap.put(OID.TEXT, text);
        oidMap.put(OID.NAME, text);
        oidMap.put(OID.BPCHAR, text);
        oidMap.put(OID.CHAR, new Char());

        // misc
        oidMap.put(OID.UUID, new UUID());
        oidMap.put(OID.JSON, new Json());
        oidMap.put(OID.JSONB, new Jsonb());
        oidMap.put(OID.BYTEA, new Bytea());
        oidMap.put(OID.BOOL, new Bool());

        // date & time
        oidMap.put(OID.TIMESTAMPTZ, new Timestamptz());
        oidMap.put(OID.TIMESTAMP, new Timestamp());
        oidMap.put(OID.DATE, new Date());
        oidMap.put(OID.TIME, new Time());
        oidMap.put(OID.TIMETZ, new Timetz());

        // arrays
        oidMap.put(OID._INT2, new Array(OID._INT2, OID.INT2));
        oidMap.put(OID._INT4, new Array(OID._INT4, OID.INT4));
        oidMap.put(OID._OID, new Array(OID._OID, OID.OID));
        oidMap.put(OID._INT8, new Array(OID._INT8, OID.INT8));
        oidMap.put(OID._NUMERIC, new Array(OID._NUMERIC, OID.NUMERIC));
        oidMap.put(OID._FLOAT4, new Array(OID._FLOAT4, OID.FLOAT4));
        oidMap.put(OID._FLOAT8, new Array(OID._FLOAT8, OID.FLOAT8));
        oidMap.put(OID._VARCHAR, new Array(OID._VARCHAR, OID.VARCHAR));
        oidMap.put(OID._TEXT, new Array(OID._TEXT, OID.TEXT));
        // oidMap.put(OID._, new Array(OID._, OID.));


        // text

//        oidMap.put(OID.NAME, text);
//        oidMap.put(OID.BPCHAR, text);
//        oidMap.put(OID.CHAR, new Char());
//
//        // misc
//        oidMap.put(OID.UUID, new UUID());
//        oidMap.put(OID.JSON, new Json());
//        oidMap.put(OID.JSONB, new Jsonb());
//        oidMap.put(OID.BYTEA, new Bytea());
//        oidMap.put(OID.BOOL, new Bool());
//
//        // date & time
//        oidMap.put(OID.TIMESTAMPTZ, new Timestamptz());
//        oidMap.put(OID.TIMESTAMP, new Timestamp());
//        oidMap.put(OID.DATE, new Date());
//        oidMap.put(OID.TIME, new Time());
//        oidMap.put(OID.TIMETZ, new Timetz());

    }

    public static IProcessor getProcessor(final int oid) {
        return oidMap.get(oid);
    }

}
