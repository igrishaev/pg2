package org.pg.reducer;

import org.pg.msg.ClojureRow;

public interface IReducer {
    Object initiate(Object[] keys);
    Object append(Object acc, ClojureRow row);
    Object finalize(Object acc);
}
