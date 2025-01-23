package org.pg.error;

public class PGBaseError extends RuntimeException {
    public PGBaseError (final String message) {
        super(message);
    }

    public PGBaseError (final Throwable e, final String message) {
        super(message, e);
    }
}
