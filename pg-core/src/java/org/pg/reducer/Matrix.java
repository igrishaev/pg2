package org.pg.reducer;

import clojure.core$persistent_BANG_;
import clojure.core$conj_BANG_;
import clojure.lang.PersistentVector;

public class Matrix extends MapMixin implements IReducer {

    public static IReducer INSTANCE = new Matrix();

    public Object compose(final Object[] keys, final Object[] vals) {
        return PersistentVector.create(vals);
    }

    public Object initiate(final Object[] keys) {
        final Object header = PersistentVector.create(keys);
        return PersistentVector.create(header).asTransient();
    }

    public Object append(final Object acc, final Object row) {
        return core$conj_BANG_.invokeStatic(acc, row);
    }

    public Object finalize(final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
