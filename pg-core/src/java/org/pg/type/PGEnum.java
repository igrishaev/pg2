package org.pg.type;

import org.pg.error.PGError;

public record PGEnum(String x) {
    public static PGEnum of(final Object x) {
        return switch (x.getClass().getCanonicalName()) {
            case "java.lang.String" -> new PGEnum((String)x);
            case "clojure.lang.Symbol" -> new PGEnum(x.toString());
            case "clojure.lang.Keyword" -> new PGEnum(x.toString().substring(1));
            default -> throw new PGError("unsupported Enum type: %s", x);
        };
    }
}
