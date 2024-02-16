package org.pg.error;


public final class PGError extends Error {

    public PGError (final String message) {
        super(message);
    }

    public PGError (final String template, final Object... args) {
        super(String.format(template, args));
    }

    public PGError (final Throwable e, final String message) {
        super(message, e);
    }

    public PGError (final Throwable e, final String template, final Object... args) {
        super(String.format(template, args), e);
    }

}
