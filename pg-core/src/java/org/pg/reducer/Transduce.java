package org.pg.reducer;

import clojure.lang.IFn;
import clojure.lang.PersistentVector;
import org.pg.clojure.LazyMap;

import clojure.core$persistent_BANG_;
import clojure.core$conj_BANG_;

public class Transduce implements IReducer {

    private final IFn tx;

    public Transduce(final IFn tx) {
        this.tx = (IFn) tx.invoke(new core$conj_BANG_());
    }

    public Object initiate(final Object[] ignored) {
        return PersistentVector.EMPTY.asTransient();
    }

    public Object append(final Object acc, final LazyMap row) {
        return tx.invoke(acc, row);
    }

    public Object finalize(final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }


}
