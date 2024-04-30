
package org.pg.reducer;

import clojure.core$assoc_BANG_;
import clojure.core$persistent_BANG_;
import clojure.lang.PersistentHashMap;
import clojure.lang.IFn;
import org.pg.clojure.LazyMap;

import java.util.Objects;

public final class KV implements IReducer {

    private final IFn fk, fv;

    public KV(final IFn fk, final IFn fv) {
        this.fk = Objects.requireNonNull(fk);
        this.fv = Objects.requireNonNull(fv);
    }

    public Object initiate (final Object[] ignored) {
        return PersistentHashMap.EMPTY.asTransient();
    }

    public Object append (final Object acc, final LazyMap row) {
        return core$assoc_BANG_.invokeStatic(acc, fk.invoke(row), fv.invoke(row));
    }

    public Object finalize (final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
