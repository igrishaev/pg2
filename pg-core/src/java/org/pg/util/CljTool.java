package org.pg.util;

import clojure.lang.IMapEntry;
import clojure.lang.Keyword;
import org.pg.error.PGError;

import java.util.Map;

public class CljTool {

    public static boolean getBool(final Map<?,?> map, final Keyword kw) {
        final Object x = map.get(kw);
        if (x instanceof Boolean b) {
            return b;
        } else if (x == null) {
            throw PGError.error("key %s is null", kw);
        } else {
            throw PGError.error("key %s is not boolean", kw);
        }
    }

    public static boolean getBool(final Map<?,?> map, final Keyword kw, final boolean notFound) {
        final Object x = map.get(kw);
        if (x instanceof Boolean b) {
            return b;
        } else if (x == null) {
            return notFound;
        } else {
            throw PGError.error("key %s is not boolean", kw);
        }
    }

    public static Iterable<?> getIterable(final Map<?,?> map, final Keyword kw) {
        final Object x = map.get(kw);
        if (x instanceof Iterable<?> i) {
            return i;
        } else if (x == null) {
            throw PGError.error("key %s is null", kw);
        } else {
            throw PGError.error("key %s is not iterable", kw);
        }
    }

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
