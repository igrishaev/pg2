package org.pg.error;

public final class PGError extends PGBaseError {

    public PGError (final String message) {
        super(message);
    }

    public PGError (final String template, final Object... args) {
        super(String.format(template, args));
    }

    public PGError (final Throwable e, final String message) {
        super(e, message);
    }

    public PGError (final Throwable e, final String template, final Object... args) {
        super(e, String.format(template, args));
    }

    public static PGError error(final String template, final Object args) {
        return new PGError(template, args);
    }

}
