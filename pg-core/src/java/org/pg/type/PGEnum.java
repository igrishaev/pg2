package org.pg.type;

import clojure.lang.Keyword;
import clojure.lang.Symbol;
import org.pg.error.PGError;

public record PGEnum(String x) {

    @SuppressWarnings("unused")
    public static PGEnum of(final Object x) {
        if (x instanceof String s) {
            return new PGEnum(s);
        } else if (x instanceof Symbol s) {
            return new PGEnum(s.toString());
        } else if (x instanceof Keyword kw) {
            return new PGEnum(kw.toString().substring(1));
        } else {
            throw new PGError("unsupported Enum type: %s", x);
        }
    }
}
