package org.pg.reducer;

import clojure.lang.AFn;
import clojure.lang.PersistentVector;
// TODO: use clojure.java.api!
import clojure.core$persistent_BANG_;
import clojure.core$conj_BANG_;

public final class Default extends AFn {

    public static Default INSTANCE = new Default();

    @Override
    public Object invoke() {
        return PersistentVector.EMPTY.asTransient();
    }

    @Override
    public Object invoke(final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }

    @Override
    public Object invoke(final Object acc, final Object row) {
        return core$conj_BANG_.invokeStatic(acc, row);
    }
}
