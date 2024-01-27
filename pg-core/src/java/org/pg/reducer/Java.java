package org.pg.reducer;

import org.pg.msg.ClojureRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Java implements IReducer {

    public static IReducer INSTANCE = new Java();

    public Object initiate(final Object[] ignored) {
        return new ArrayList<>();
    }

    public Object append(final Object acc, final ClojureRow row) {
        @SuppressWarnings("unchecked") final List<Object> _acc = (List<Object>) acc;
        _acc.add(row);
        return _acc;
    }

    public Object finalize(final Object acc) {
        return acc;
    }

}
