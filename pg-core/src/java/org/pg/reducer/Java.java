package org.pg.reducer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Java implements IReducer {

    public static IReducer INSTANCE = new Java();

    public Object compose(Object[] keys, Object[] vals) {
        final Map<Object, Object> result = new HashMap<>(keys.length);
        for (int i = 0; i < keys.length; i++) {
            result.put(keys[i], vals[i]);
        }
        return result;
    }

    public Object initiate(final Object[] ignored) {
        return new ArrayList<>();
    }

    public Object append(final Object acc, final Object row) {
        @SuppressWarnings("unchecked") final List<Object> _acc = (List<Object>) acc;
        _acc.add(row);
        return _acc;
    }

    public Object finalize(final Object acc) {
        return acc;
    }

}
