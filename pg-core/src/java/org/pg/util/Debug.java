package org.pg.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public final class Debug {

    public static final boolean isON = System.getenv("PG_DEBUG") != null;

    public static void debug(final String template, final Object... args) {
        System.out.printf((template) + "%n", args);
        System.out.flush();
    }

    public static void main(final String[] args) {

        CompletableFuture<Integer> fut = new CompletableFuture<>();
        CompletableFuture<Integer> fut2 = fut.thenApply((Integer val) -> val + 1).complete;

        try {
            System.out.println(fut2.get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }


    }

}
