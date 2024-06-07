package org.pg.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public final class Debug {

    public static final boolean isON = System.getenv("PG_DEBUG") == "1";

    public static void debug(final String template, final Object... args) {
        System.out.printf((template) + "%n", args);
        System.out.flush();}


}
