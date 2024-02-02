
package org.pg.reducer;

import clojure.lang.IFn;
import org.pg.clojure.LazyMap;

import java.util.Objects;

public class Fold implements IReducer {

    private final IFn f;
    private final Object init;

    public Fold(final IFn f, final Object init) {
        this.f = Objects.requireNonNull(f);
        this.init = init;
    }

    public Object initiate (final Object[] ignored) {
        return init;
    }

    public Object append (final Object acc, final LazyMap row) {
        return f.invoke(acc, row);
    }

    public Object finalize (final Object acc) {
        return acc;
    }
}
