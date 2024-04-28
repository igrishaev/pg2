package org.pg.reducer;

import clojure.lang.PersistentVector;
import org.pg.clojure.LazyMap;
import clojure.core$persistent_BANG_;
import clojure.core$conj_BANG_;

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
        return core$conj_BANG_.invokeStatic(acc, value);
    }

    @Override
    public Object finalize(final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
