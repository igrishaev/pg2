
package org.pg.reducer;

import clojure.lang.IFn;

import java.util.Objects;

public class Fold extends MapMixin implements IReducer {

    private final IFn f;
    private final Object init;

    public Fold(final IFn f, final Object init) {
        this.f = Objects.requireNonNull(f);
        this.init = init;
    }

    public Object initiate () {
        return init;
    }

    public Object append (final Object acc, final Object row) {
        return f.invoke(acc, row);
    }

    public Object finalize (final Object acc) {
        return acc;
    }
}
