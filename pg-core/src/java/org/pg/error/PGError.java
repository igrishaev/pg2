package org.pg.error;

public class PGError extends RuntimeException {

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

    public static PGError error(final String template, final Object args) {
        return new PGError(template, args);
    }

}
