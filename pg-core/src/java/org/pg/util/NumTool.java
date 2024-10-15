package org.pg.util;

import org.pg.error.PGError;

public class NumTool {

    public static short toShort(final Number n) {
        final long l = n.longValue();
        if (Short.MIN_VALUE <= l && l <= Short.MAX_VALUE) {
            return n.shortValue();
        } else {
            throw new PGError("number %s is out of INT2 range", n);
        }
    }

    public static int toInteger(final Number n) {
        final long l = n.longValue();
        if (Integer.MIN_VALUE <= l && l <= Integer.MAX_VALUE) {
            return n.intValue();
        } else {
            throw new PGError("number %s is out of INT4 range", n);
        }
    }

    public static long toLong(final Number n) {
        return n.longValue();
    }

    public static float toFloat(final Number n) {
        final double d = n.doubleValue();
        if (Float.MIN_VALUE <= d && d <= Float.MAX_VALUE) {
            return n.floatValue();
        } else {
            throw new PGError("number %s is out of FLOAT4 range", n);
        }
    }

    public static double toDouble(final Number n) {
        return n.doubleValue();
    }

}
