package org.pg.clojure;

import clojure.lang.*;
import clojure.lang.PersistentVector;
import clojure.lang.IPersistentCollection;
import clojure.core$nth;

import java.util.ArrayList;

public final class ClojureVector extends ArrayList<Object> implements IPersistentVector {

    public PersistentVector toVector() {
        return PersistentVector.create(this);
    }

    @Override
    public int length() {
        return this.size();
    }

    @Override
    public IPersistentVector assocN(final int i, final Object val) {
        return toVector().assocN(i, val);
    }

    @Override
    public int count() {
        return this.size();
    }

    @Override
    public IPersistentVector cons(final Object o) {
        return toVector().cons(o);
    }

    @Override
    public IPersistentCollection empty() {
        return PersistentVector.EMPTY;
    }

    @Override
    public boolean equiv(final Object o) {
        return toVector().equiv(o);
    }

    @Override
    public boolean containsKey(final Object key) {
        if (key instanceof Integer i) {
            return this.contains(i);
        } else {
            return false;
        }
    }

    @Override
    public IMapEntry entryAt(final Object key) {
        if (key instanceof Integer i) {
            if (this.contains(i)) {
                return new MapEntry(i, get(i));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public Associative assoc(final Object key, final Object val) {
        return toVector().assoc(key, val);
    }

    @Override
    public Object valAt(final Object key) {
        if (key instanceof Integer i) {
            return get(i);
        } else {
            return null;
        }
    }

    @Override
    public Object valAt(final Object key, final Object notFound) {
        if (key instanceof Integer i) {
            if (this.contains(i)) {
                return get(i);
            } else {
                return notFound;
            }
        } else {
            return null;
        }
    }

    @Override
    public Object peek() {
        final int len = size();
        if (len > 0) {
            return get(len - 1);
        } else {
            return null;
        }
    }

    @Override
    public IPersistentStack pop() {
        return toVector().pop();
    }

    @Override
    public Object nth(final int i) {
        return core$nth.invokeStatic(this, i);
    }

    @Override
    public Object nth(int i, final Object notFound) {
        return core$nth.invokeStatic(this, i, notFound);
    }

    @Override
    public ISeq rseq() {
        return null;
    }

    @Override
    public ISeq seq() {
        return null;
    }
}
