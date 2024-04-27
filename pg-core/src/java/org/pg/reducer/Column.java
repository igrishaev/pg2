package org.pg.reducer;

import clojure.lang.PersistentVector;
import org.pg.clojure.LazyMap;

public final class Column implements IReducer {

    private final Object column;

    public Column(final Object column) {
        this.column = column;
    }

    @Override
    public Object initiate(final Object[] keys) {
        return PersistentVector.EMPTY.asTransient();
    }

    @Override
    public Object append(final Object acc, LazyMap row) {
        final Object value = row.get(column);
        return clojure.core$conj_BANG_.invokeStatic(acc, value);
    }

    @Override
    public Object finalize(final Object acc) {
        return clojure.core$persistent_BANG_.invokeStatic(acc);
    }
}
