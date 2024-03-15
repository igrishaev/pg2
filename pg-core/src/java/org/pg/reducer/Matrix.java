package org.pg.reducer;

import clojure.core$persistent_BANG_;
import clojure.core$conj_BANG_;
import clojure.lang.PersistentVector;
import org.pg.clojure.LazyMap;

public class Matrix implements IReducer {

    public static IReducer INSTANCE = new Matrix();

    public Object initiate(final Object[] keys) {
        final Object header = PersistentVector.create(keys);
        return PersistentVector.create(header).asTransient();
    }

    public Object append(final Object acc, final LazyMap row) {
        return core$conj_BANG_.invokeStatic(acc, row.toLazyVector());
    }

    public Object finalize(final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
