package org.pg.reducer;

import org.pg.clojure.ClojureVector;
import org.pg.clojure.LazyMap;

public final class Default implements IReducer {

    public final static IReducer INSTANCE = new Default();

    public Object initiate(final Object[] ignored) {
        return new ClojureVector();
    }

    public Object append(final Object acc, final LazyMap row) {
        ((ClojureVector)acc).add(row);
        return acc;
    }

    public Object finalize(final Object acc) {
        return acc;
    }
}
