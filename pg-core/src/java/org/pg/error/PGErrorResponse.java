package org.pg.error;

import clojure.lang.IExceptionInfo;
import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import org.pg.clojure.KW;
import org.pg.msg.server.ErrorResponse;
import org.pg.util.StrTool;

import java.util.Map;

public final class PGErrorResponse extends PGBaseError implements IExceptionInfo {

    private final ErrorResponse errorResponse;
    private final String sql;

    public PGErrorResponse (final ErrorResponse errorResponse) {
        this(errorResponse, null);
    }

    public PGErrorResponse (final ErrorResponse errorResponse, final String sql) {
        super(String.format(
                "Server error response: %s, sql: %s",
                errorResponse.fields(),
                sql == null ? "<unknown>" : StrTool.truncate(sql)
        ));
        this.errorResponse = errorResponse;
        this.sql = sql;
    }

    /**
     * Get the "code" filed of an error message returned
     * by the server. Can be null. In rare cases, logic
     * might depend on what was the cause of an error.
     */
    public String getCode () {
        return errorResponse.fields().get("code");
    }

    @Override
    public IPersistentMap getData() {
        final Map<String, String> fields = errorResponse.fields();
        final Object[] items = new Object[fields.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> me: fields.entrySet()) {
            items[i * 2] = Keyword.intern(me.getKey());
            items[i * 2 + 1] = me.getValue();
            i++;
        }
        return PersistentHashMap.create(items).assoc(KW.sql, sql);
    }
}
