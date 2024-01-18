
package org.pg.reducer;

import clojure.lang.IFn;
import java.util.Objects;

public class Run extends MapMixin implements IReducer {

    private final IFn f;

    public Run(final IFn f) {
        this.f = Objects.requireNonNull(f);
    }

    public Object initiate (final Object[] ignored) {
        return 0;
    }

    public Object append (final Object acc, final Object row) {
        f.invoke(row);
        return (Integer) acc + 1;
    }

    public Object finalize (final Object acc) {
        return acc;
    }
}
