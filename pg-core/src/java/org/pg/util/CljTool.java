package org.pg.util;

import clojure.lang.IMapEntry;
import org.pg.error.PGError;

@SuppressWarnings("unused")
public class CljTool {

    public static IMapEntry mapEntry(final Object key, final Object val) {
        return new IMapEntry() {
            @Override
            public Object key() {
                return key;
            }
            @Override
            public Object val() {
                return val;
            }
            @Override
            public Object getKey() {
                return key;
            }
            @Override
            public Object getValue() {
                return val;
            }
            @Override
            public Object setValue(Object value) {
                throw new PGError("setValue is not implemented");
            }
        };
    }

}
