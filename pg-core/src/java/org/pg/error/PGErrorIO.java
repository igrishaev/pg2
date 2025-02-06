package org.pg.error;

public final class PGErrorIO extends PGError {

    @SuppressWarnings("unused")
    public PGErrorIO(final String message) {
        super(message);
    }

    public PGErrorIO(final Throwable e, final String message) {
        super(e, message);
    }

    public PGErrorIO(final Throwable e, final String template, final Object... args) {
        super(e, String.format(template, args));
    }
}
