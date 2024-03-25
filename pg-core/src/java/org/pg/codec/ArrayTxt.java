package org.pg.codec;

import clojure.lang.IPersistentVector;
import clojure.lang.Sequential;
import org.pg.enums.OID;

public final class ArrayTxt {

    public static String quoteElement(final String element) {
        final StringBuilder sb = new StringBuilder();
        final int len = element.length();
        char c;
        for (int i = 0; i < len; i++) {
            c = element.charAt(i);
            sb.append(switch (c) {
                case '"' -> "\\\"";
                case '\\' -> "\\\\";
                default -> c;
            });
        }
        return sb.toString();
    }

    public static String encode (final Object x, final OID oidArray, final CodecParams codecParams) {

        if (x instanceof Sequential s) {
            final StringBuilder sb = new StringBuilder();
            return "aaa";
        } else if (x == null) {
            return "NULL";
        } else {
            final OID oidEl = oidArray.toElementOID();
            return quoteElement(EncoderTxt.encode(x, oidEl, codecParams));
        }

    }
}
