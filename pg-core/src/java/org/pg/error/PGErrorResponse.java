package org.pg.error;

import clojure.lang.IPersistentCollection;
import clojure.lang.IExceptionInfo;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import org.pg.clojure.KW;
import org.pg.msg.server.ErrorResponse;

public final class PGErrorResponse extends RuntimeException implements IExceptionInfo {

    private final ErrorResponse errorResponse;
    private final String sql;

    public PGErrorResponse (final ErrorResponse errorResponse) {
        this(errorResponse, null);
    }

    public PGErrorResponse (final ErrorResponse errorResponse, final String sql) {
        super(String.format("Server error response: %s", errorResponse.fields()));
        this.errorResponse = errorResponse;
        this.sql = sql;
    }

    @Override
    public IPersistentMap getData() {
        return PersistentHashMap.create(errorResponse.fields())
                .assoc(KW.query, sql);
    }
}
