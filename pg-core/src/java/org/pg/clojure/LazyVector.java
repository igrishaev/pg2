package org.pg.clojure;

import clojure.lang.*;
import org.pg.codec.CodecParams;
import org.pg.msg.DataRow;
import org.pg.msg.RowDescription;

import java.util.HashMap;
import java.util.Map;

public class LazyVector extends APersistentVector {

    private final DataRow dataRow;
    private final RowDescription rowDescription;
    private final CodecParams codecParams;
    private final Map<Integer, Object> parsedValues;

    public LazyVector (final DataRow dataRow,
                       final RowDescription rowDescription,
                       final CodecParams codecParams) {
        this.dataRow = dataRow;
        this.rowDescription = rowDescription;
        this.codecParams = codecParams;
        this.parsedValues = new HashMap<>(dataRow.valueCount());
    }

    @Override
    public IPersistentVector assocN(int i, Object val) {
        return null;
    }

    @Override
    public int count() {
        return dataRow.valueCount();
    }

    @Override
    public IPersistentVector cons(Object o) {
        return null;
    }

    @Override
    public IPersistentCollection empty() {
        return PersistentVector.EMPTY;
    }

    @Override
    public IPersistentStack pop() {
        return null;
    }

    @Override
    public Object nth(int i) {
        return null;
    }
}
