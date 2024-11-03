package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;

public record Point (float x, float y) implements Counted, Indexed, ILookup, IDeref {

    @Override
    public Object nth(final int i) {
        return switch (i) {
            case 0 -> x;
            case 1 -> y;
            default -> throw new IndexOutOfBoundsException("index is out of range: " + i);
        };
    }

    @Override
    public Object nth(final int i, final Object notFound) {
        return switch (i) {
            case 0 -> x;
            case 1 -> y;
            default -> notFound;
        };
    }

    @Override
    public int count() {
        return 2;
    }

    @Override
    public Object valAt(final Object key) {
        return valAt(key, null);
    }

    @Override
    public Object valAt(final Object key, final Object notFound) {
        if (key instanceof Keyword kw) {
            if (kw == KW.x) {
                return x;
            } else if (kw == KW.y) {
                return y;
            }
        }
        return notFound;
    }

    @Override
    public Object deref() {
        return PersistentVector.create(x, y);
    }
}
