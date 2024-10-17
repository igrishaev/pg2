package org.pg.util;

public class TypeTool {

    public static String className(final Object x) {
        if (x == null) {
            return "NULL";
        } else {
            return x.getClass().getName();
        }
    }

    public static String repr(final Object x) {
        if (x == null) {
            return "NULL";
        } else {
            return String.format("type: %s, value: %s", x.getClass().getName(), x);
        }
    }


}
