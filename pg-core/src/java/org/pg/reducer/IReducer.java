package org.pg.reducer;

import org.pg.clojure.LazyMap;

public interface IReducer {
    Object initiate(Object[] keys);
    Object append(Object acc, LazyMap row);
    Object finalize(Object acc);
}
