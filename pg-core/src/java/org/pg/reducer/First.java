package org.pg.reducer;

import org.pg.clojure.LazyMap;
import org.pg.proto.IReducer;

public class First implements IReducer {

    public static IReducer INSTANCE = new First();

    public Object initiate (final Object[] ignored) {
        return new Object[1];
    }

    public Object append (final Object obj, final LazyMap row) {
        final Object[] acc = (Object[]) obj;
        if (acc[0] == null) {
            acc[0] = row;
        }
        return acc;
    }

    public Object finalize (final Object obj) {
        final Object[] acc = (Object[]) obj;
        return acc[0];
    }
}
