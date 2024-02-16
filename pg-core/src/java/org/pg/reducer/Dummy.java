package org.pg.reducer;

import org.pg.clojure.LazyMap;
import org.pg.proto.IReducer;

public class Dummy implements IReducer {

    public static IReducer INSTANCE = new Dummy();

    public Object initiate(final Object[] ignored) {
        return null;
    }

    public Object append(final Object acc, final LazyMap row) {
        return acc;
    }

    public Object finalize(final Object acc) {
        return acc;
    }
}
