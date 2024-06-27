package org.pg.reducer;

import clojure.lang.AFn;
import clojure.lang.PersistentVector;
import clojure.core$persistent_BANG_;
import clojure.core$conj_BANG_;

public final class Foo extends AFn {

    public static Foo INSTANCE = new Foo();

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
