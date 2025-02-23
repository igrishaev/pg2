package org.pg.util;

import clojure.lang.RT;
import org.pg.error.PGError;

public class NumTool {

    public static short toShort(final Object x) {
        if (x instanceof Number n) {
            return RT.shortCast(n);
        } else {
            throw new PGError("cannot coerce value to short: %s", TypeTool.repr(x));
        }
    }

    public static int toInteger(final Object x) {
        if (x instanceof Number n) {
            return RT.intCast(n);
        } else {
            throw new PGError("cannot coerce value to integer: %s", TypeTool.repr(x));
        }
    }

    public static long toLong(final Object x) {
        if (x instanceof Number n) {
            return RT.longCast(n);
        } else {
            throw new PGError("cannot coerce value to long: %s", TypeTool.repr(x));
        }
    }

    public static float toFloat(final Object x) {
        if (x instanceof Number n) {
            return RT.floatCast(n);
        } else {
            throw new PGError("cannot coerce value to float: %s", TypeTool.repr(x));
        }
    }

    public static double toDouble(final Object x) {
        if (x instanceof Number n) {
            return RT.doubleCast(n);
        } else {
            throw new PGError("cannot coerce value to double: %s", TypeTool.repr(x));
        }
    }
}
