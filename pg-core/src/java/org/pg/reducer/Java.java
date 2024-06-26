package org.pg.reducer;

import org.pg.clojure.LazyMap;

import java.util.ArrayList;
import java.util.List;

public final class Java implements IReducer {

    public static IReducer INSTANCE = new Java();

    public Object initiate(final Object[] ignored) {
        return new ArrayList<>();
    }

    public Object append(final Object acc, final LazyMap row) {
        @SuppressWarnings("unchecked") final List<Object> _acc = (List<Object>) acc;
        _acc.add(row.toJavaMap());
        return _acc;
    }

    public Object finalize(final Object acc) {
        return acc;
    }

}
