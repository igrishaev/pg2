
package org.pg.reducer;

import clojure.lang.RT;
import clojure.lang.PersistentHashMap;
import clojure.lang.IFn;
import clojure.lang.PersistentVector;

import java.util.Objects;

public class GroupBy extends MapMixin implements IReducer {

    private final IFn f;

    public GroupBy(final IFn f) {
        this.f = Objects.requireNonNull(f);
    }

    public Object initiate (final Object[] ignored) {
        return PersistentHashMap.EMPTY;
    }

    public Object append (final Object acc, final Object row) {
        final Object key = f.invoke(row);
        if (RT.contains(acc, key) == Boolean.FALSE) {
            return RT.assoc(acc, key, PersistentVector.EMPTY.cons(row));
        }
        else {
            final PersistentVector vec = (PersistentVector) RT.get(acc, key);
            return RT.assoc(acc, key, vec.cons(row));
        }
    }

    public Object finalize (final Object acc) {
        return acc;
    }
}
