package org.pg.error;

import clojure.lang.IPersistentCollection;
import org.pg.msg.server.ErrorResponse;

public final class PGErrorResponse extends RuntimeException {

    private final ErrorResponse errorResponse;

    @SuppressWarnings("unused")
    public IPersistentCollection getErrorFields () {
        return this.errorResponse.toClojure();
    }

    public PGErrorResponse (final ErrorResponse errorResponse) {
        super(String.format("Server error response: %s", errorResponse.fields()));
        this.errorResponse = errorResponse;
    }

}
