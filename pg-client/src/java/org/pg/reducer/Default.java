package org.pg.reducer;

import clojure.core$persistent_BANG_;
import clojure.core$conj_BANG_;
import clojure.lang.PersistentVector;

public class Default extends MapMixin implements IReducer {

    public static IReducer INSTANCE = new Default();

    public Object initiate() {
        return PersistentVector.EMPTY.asTransient();
    }

    public Object append(final Object acc, final Object row) {
        return core$conj_BANG_.invokeStatic(acc, row);
    }

    public Object finalize(final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
