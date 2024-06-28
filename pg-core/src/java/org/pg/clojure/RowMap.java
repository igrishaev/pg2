package org.pg.clojure;

import clojure.lang.*;
import org.pg.codec.CodecParams;
import org.pg.codec.DecoderBin;
import org.pg.codec.DecoderTxt;
import org.pg.msg.server.DataRow;
import org.pg.msg.server.RowDescription;
import org.pg.util.TryLock;

import java.nio.ByteBuffer;
import java.util.*;

public final class RowMap extends APersistentMap implements IDeref {

    private int[] ToC = null;
    private final TryLock lock;
    private final DataRow dataRow;
    private final RowDescription rowDescription;
    private final Object[] keys;
    private final Map<Object, Short> keysIndex;
    private final CodecParams codecParams;
    private final Object[] parsedValues;
    private final boolean[] parsedKeys;

    public RowMap(final TryLock lock,
                  final DataRow dataRow,
                  final RowDescription rowDescription,
                  final Object[] keys,
                  final Map<Object, Short> keysIndex,
                  final CodecParams codecParams
    ) {
        final int count = dataRow.count();
        this.lock = lock;
        this.dataRow = dataRow;
        this.rowDescription = rowDescription;
        this.keys = keys;
        this.keysIndex = keysIndex;
        this.codecParams = codecParams;
        this.parsedValues = new Object[count];
        this.parsedKeys = new boolean[count];
    }

    private IPersistentMap toClojureMap() {
        ITransientMap result = PersistentHashMap.EMPTY.asTransient();
        for (final Map.Entry<Object, Short> mapEntry: keysIndex.entrySet()) {
            result = result.assoc(mapEntry.getKey(), getValueByIndex(mapEntry.getValue()));
        }
        return result.persistent();
    }

    @SuppressWarnings("unused")
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

        try (TryLock ignored = lock.get()) {
            return getValueByIndex_unlocked(i);
        }
    }

    private Object getValueByIndex_unlocked (final int i) {

        if (ToC == null) {
            ToC = dataRow.ToC();
        }

        final int offset = ToC[i * 2];
        final int length = ToC[i * 2 + 1];

        if (length == -1) {
            parsedKeys[i] = true;
            parsedValues[i] = null;
            return null;
        }

        final byte[] payload = dataRow.buf().array();
        final RowDescription.Column col = rowDescription.columns()[i];

        final Object value = switch (col.format()) {
            case TXT -> {
                final String string = new String(payload, offset, length, codecParams.serverCharset());
                yield DecoderTxt.decode(string, col.typeOid(), codecParams);
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

    @SuppressWarnings("unused")
    public IPersistentVector keys () {
        return PersistentVector.create(keys);
    }

    @SuppressWarnings("unused")
    public IPersistentCollection vals () {
        ITransientCollection result = PersistentVector.EMPTY.asTransient();
        for (final Object key: keys) {
            result = result.conj(getValueByKey(key));
        }
        return result.persistent();
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

    @Override
    public Object deref() {
        return toClojureMap();
    }
}
