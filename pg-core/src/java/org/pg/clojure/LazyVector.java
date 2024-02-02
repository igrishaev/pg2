package org.pg.clojure;

import clojure.lang.APersistentVector;
import clojure.lang.PersistentVector;
import clojure.lang.IPersistentVector;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentStack;


public class LazyVector extends APersistentVector {

    private final LazyMap lazyMap;

    public LazyVector (final LazyMap lazyMap) {
        this.lazyMap = lazyMap;
    }

    private PersistentVector toVector() {
        lazyMap.parseAll();
        return PersistentVector.adopt(lazyMap.getParsedValues());
    }

    @Override
    public IPersistentVector assocN(final int i, final Object val) {
        return toVector().assocN(i, val);
    }

    @Override
    public int count() {
        return lazyMap.count();
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
    public IPersistentStack pop() {
        return toVector().pop();
    }

    @Override
    public Object nth(int i) {
        return lazyMap.getValueByIndex(i);
    }
}
