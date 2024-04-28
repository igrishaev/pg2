package org.pg.clojure;

import clojure.lang.*;
import clojure.lang.PersistentVector;
import clojure.lang.IPersistentCollection;
import java.util.ArrayList;
import java.util.Iterator;


public final class ClojureVector extends ArrayList<Object> implements IPersistentVector {

    @SuppressWarnings("unused")
    public ClojureVector() {
        super();
    }

    @SuppressWarnings("unused")
    public ClojureVector(final int size) {
        super(size);
    }

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
        if (this == o) {
            return true;
        }

        if (!RT.canSeq(o)) {
            return false;
        }

        final Iterator<Object> iter1 = this.iterator();
        final Iterator<?> iter2 = RT.iter(o);

        while (iter1.hasNext()) {
            if (!iter2.hasNext()) {
                return false;
            }
            if (!Util.equiv(iter1.next(), iter2.next())) {
                return false;
            }
        }
        return !iter2.hasNext();
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
        return valAt(key, null);
    }

    @Override
    public Object valAt(final Object key, final Object notFound) {
        if (key instanceof Integer i) {
            return nth(i, notFound);
        } else if (key instanceof Long l) {
            return nth(l.intValue(), notFound);
        } else if (key instanceof Short s) {
            return nth(s.intValue(), notFound);
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
        if (0 <= i && i < this.size()) {
            return get(i);
        } else {
            return null;
        }
    }

    @Override
    public Object nth(int i, final Object notFound) {
        if (0 <= i && i < this.size()) {
            return get(i);
        } else {
            return notFound;
        }
    }

    @Override
    public ISeq rseq() {
        return toVector().rseq();
    }

    @Override
    public ISeq seq() {
        return RT.chunkIteratorSeq(this.iterator());
    }

    public static void main(String[] args) {
        ClojureVector v = new ClojureVector();
        v.add("aa");
        v.add("bb");
        System.out.println(RT.get(v, Long.parseLong("0")));
        System.out.println(v.equiv(PersistentVector.create("aa", "bb")));
    }
}
