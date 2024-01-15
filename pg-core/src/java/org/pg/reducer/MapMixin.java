package org.pg.reducer;

import clojure.lang.ITransientAssociative;
import clojure.lang.PersistentHashMap;

public abstract class MapMixin implements IReducer {

    public Object compose(final Object[] keys, final Object[] vals) {
        ITransientAssociative map = PersistentHashMap.EMPTY.asTransient();
        for (short i = 0; i < keys.length; i++) {
            map = map.assoc(keys[i], vals[i]);
        }
        return map.persistent();
    }

}
