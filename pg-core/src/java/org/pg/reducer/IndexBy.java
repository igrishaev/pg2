
package org.pg.reducer;

import clojure.core$assoc_BANG_;
import clojure.core$persistent_BANG_;
import clojure.lang.PersistentHashMap;
import clojure.lang.IFn;
import org.pg.clojure.LazyMap;

import java.util.Objects;

public final class IndexBy implements IReducer {

    private final IFn f;

    public IndexBy(final IFn f) {
        this.f = Objects.requireNonNull(f);
    }

    public Object initiate (final Object[] ignored) {
        return PersistentHashMap.EMPTY.asTransient();
    }

    public Object append (final Object acc, final LazyMap row) {
        return core$assoc_BANG_.invokeStatic(acc, f.invoke(row), row);
    }

    public Object finalize (final Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
