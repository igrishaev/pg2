package org.pg.util;

public final class Debug {

    public static final boolean isON = System.getenv("PG_DEBUG") != null;

    public static void debug(final String template, final Object... args) {
        System.out.printf((template) + "%n", args);
    }

}
