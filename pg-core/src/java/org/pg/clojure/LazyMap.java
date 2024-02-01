package org.pg.clojure;

import clojure.lang.*;
import org.pg.codec.CodecParams;
import org.pg.codec.DecoderBin;
import org.pg.codec.DecoderTxt;
import org.pg.msg.DataRow;
import org.pg.msg.RowDescription;

import java.nio.ByteBuffer;
import java.util.*;

public final class LazyMap extends APersistentMap {

    private final DataRow dataRow;
    private final RowDescription rowDescription;
    private final Map<Object, Short> keysIndex;
    private final CodecParams codecParams;
    private final Object[] parsedValues;
    private final boolean[] parsedKeys;

    public LazyMap(final DataRow dataRow,
                   final RowDescription rowDescription,
                   final Map<Object, Short> keysIndex,
                   final CodecParams codecParams
    ) {
        final int count = dataRow.count();
        this.dataRow = dataRow;
        this.rowDescription = rowDescription;
        this.keysIndex = keysIndex;
        this.codecParams = codecParams;
        this.parsedValues = new Object[count];
        this.parsedKeys = new boolean[count];
    }

    public LazyVector toLazyVector () {
        return new LazyVector(this);
    }

    private IPersistentMap toClojureMap() {
        ITransientMap result = PersistentHashMap.EMPTY.asTransient();
        for (final Map.Entry<Object, Short> mapEntry: keysIndex.entrySet()) {
            result = result.assoc(mapEntry.getKey(), getValueByIndex(mapEntry.getValue()));
        }
        return result.persistent();
    }

    public Map<Object, Object> toJavaMap() {
        final Map<Object, Object> result = new HashMap<>(keysIndex.size());
        for (final Map.Entry<Object, Short> mapEntry: keysIndex.entrySet()) {
            result.put(mapEntry.getKey(), getValueByIndex(mapEntry.getValue()));
        }
        return result;
    }

    public Object getValueByIndex (final int i) {

        if (i < 0 || i >= parsedKeys.length) {
            return null;
        }

        if (parsedKeys[i]) {
            return parsedValues[i];
        }

        final int[][] ToC = dataRow.ToC();

        final int offset = ToC[i][0];
        final int length = ToC[i][1];

        if (length == -1) {
            parsedKeys[i] = true;
            parsedValues[i] = null;
            return null;
        }

        final byte[] payload = dataRow.payload();
        final RowDescription.Column col = rowDescription.columns()[i];

        final Object value = switch (col.format()) {
            case TXT -> {
                final String string = new String(payload, offset, length, codecParams.serverCharset);
                yield DecoderTxt.decode(string, col.typeOid());
            }
            case BIN -> {
                final ByteBuffer buf = ByteBuffer.wrap(payload, offset, length);
                yield DecoderBin.decode(buf, col.typeOid(), codecParams);
            }
        };

        parsedKeys[i] = true;
        parsedValues[i] = value;

        return value;
    }


    private Object getValueByKey (final Object key) {
        if (!keysIndex.containsKey(key)) {
            return null;
        }
        final int i = keysIndex.get(key);
        return getValueByIndex(i);
    }

    @Override
    public boolean containsKey(final Object key) {
        return keysIndex.containsKey(key);
    }

    @Override
    public IMapEntry entryAt(final Object key) {
        final Object value = getValueByKey(key);
        return new MapEntry(key, value);
    }

    @Override
    public IPersistentMap assoc(final Object key, final Object val) {
        return toClojureMap().assoc(key, val);
    }

    @Override
    public IPersistentMap assocEx(final Object key, final Object val) {
        return toClojureMap().assocEx(key, val);
    }

    @Override
    public IPersistentMap without(final Object key) {
        return toClojureMap().without(key);
    }

    @Override
    public Object valAt(final Object key) {
        return getValueByKey(key);
    }

    @Override
    public Object valAt(final Object key, final Object notFound) {
        if (containsKey(key)) {
            return getValueByKey(key);
        }
        else {
            return notFound;
        }
    }

    @Override
    public int count() {
        return dataRow.count();
    }

    @Override
    public IPersistentCollection empty() {
        return PersistentHashMap.EMPTY;
    }

    @Override
    public ISeq seq() {
        return toClojureMap().seq();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Iterator iterator() {
        return toClojureMap().iterator();
    }

}
