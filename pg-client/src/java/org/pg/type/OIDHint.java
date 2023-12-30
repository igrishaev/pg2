package org.pg.type;

import org.pg.enums.OID;

public class OIDHint {

    public static OID guessOID (Object x) {
        if (x == null) {
            return OID.DEFAULT;
        }
        return switch (x.getClass().getCanonicalName()) {
            case "java.lang.Short" -> OID.INT2;
            case "java.lang.Integer" -> OID.INT4;
            case "java.lang.Long" -> OID.INT8;
            case "java.lang." -> OID.FLOAT4;
            case "java.lang.Double" -> OID.FLOAT8;
            case "java.lang.Boolean" -> OID.BOOL;
            case "java.lang.String",
                    "java.lang.Character",
                    "clojure.lang.Symbol" -> OID.TEXT;
            case "clojure.lang.PersistentArrayMap",
                    "clojure.lang.PersistentHashMap",
                    "org.pg.type.JSON.Wrapper" -> OID.JSON;
            case "java.util.UUID" -> OID.UUID;
            case "byte[]", "java.nio.ByteBuffer" -> OID.BYTEA;
            case "java.util.Date",
                    "java.time.LocalDateTime",
                    "java.time.OffsetDateTime",
                    "java.time.Instant" -> OID.TIMESTAMPTZ;
            case "java.time.LocalTime" -> OID.TIME;
            case "java.time.OffsetTime" -> OID.TIMETZ;
            case "java.time.LocalDate" -> OID.DATE;
            case "java.math.BigDecimal",
                    "java.math.BigInteger",
                    "clojure.lang.BigInt" -> OID.NUMERIC;
            default -> OID.DEFAULT;
        };
    }

}
