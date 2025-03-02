package org.pg.reducer;

import clojure.lang.AFn;
import clojure.lang.ITransientVector;
import clojure.lang.PersistentVector;

public final class Default extends AFn {

    public static Default INSTANCE = new Default();

    @Override
    public Object invoke() {
        return PersistentVector.EMPTY.asTransient();
    }

    @Override
    public Object invoke(final Object acc) {
        return ((ITransientVector) acc).persistent();
    }

    @Override
    public Object invoke(final Object acc, final Object row) {
        return ((ITransientVector) acc).conj(row);
    }
}
