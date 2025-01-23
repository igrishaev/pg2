package org.pg.error;

public final class PGIOException extends PGBaseError {
    public PGIOException (final String message) {
        super(message);
    }

    public PGIOException (final Throwable e, final String message) {
        super(e, message);
    }

    public PGIOException (final Throwable e, final String template, final Object... args) {
        super(e, String.format(template, args));
    }
}
