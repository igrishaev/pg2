package org.pg.clojure;

import clojure.lang.*;
import org.pg.codec.CodecParams;
import org.pg.msg.server.DataRow;
import org.pg.msg.server.RowDescription;
import org.pg.processor.IProcessor;
import org.pg.processor.Processors;
import org.pg.util.TryLock;

import java.nio.ByteBuffer;
import java.util.*;

public final class RowMap extends APersistentMap implements Indexed {

    private int[] ToC = null;
    private final int count;
    private final TryLock lock;
    private final DataRow dataRow;
    private final RowDescription rowDescription;
    private final Object[] keys;
    private final Map<Object, Short> keysIndex;
    private final CodecParams codecParams;
    private final Object[] parsedValues;
    private final boolean[] parsedKeys;

//    static {
//        CljAPI.preferMethod.invoke(
//                CljAPI.printMethod,
//                clojure.lang.IPersistentMap.class,
//                clojure.lang.IDeref.class
//        );
//    }

    public RowMap(final DataRow dataRow,
                  final RowDescription rowDescription,
                  final Object[] keys,
                  final Map<Object, Short> keysIndex,
                  final CodecParams codecParams
    ) {
        this.count = dataRow.count();
        this.lock = new TryLock();
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
        short i;
        for (final Map.Entry<Object, Short> mapEntry: keysIndex.entrySet()) {
            i = mapEntry.getValue();
            result = result.assoc(mapEntry.getKey(), getValueByIndex(i));
        }
        return result.persistent();
    }

    @SuppressWarnings("unused") // pg.fold
    public Map<Object, Object> toJavaMap() {
        final Map<Object, Object> result = new HashMap<>(keysIndex.size());
        short i;
        for (final Map.Entry<Object, Short> mapEntry: keysIndex.entrySet()) {
            i = mapEntry.getValue();
            result.put(mapEntry.getKey(), getValueByIndex(i));
        }
        return result;
    }

    private boolean missesIndex(final int i) {
        return 0 > i || i >= count;
    }

    private Object getValueByIndex(final int i) {
        if (parsedKeys[i]) {
            return parsedValues[i];
        }
        try (TryLock ignored = lock.get()) {
            return getValueByIndexUnlocked(i);
        }
    }

    private MapEntry entryById(final int i) {
        return new MapEntry(keys[i], getValueByIndex(i));
    }

    private class EntrySeq extends ASeq {
        private final int i;
        public EntrySeq(final int i) {
            this.i = i;
        }
        public EntrySeq(final int i, final IPersistentMap meta) {
            super(meta);
            this.i = i;
        }
        @Override
        public Object first() {
            return RowMap.this.entryById(i);
        }
        @Override
        public ISeq next() {
            final int iNext = i + 1;
            if (missesIndex(iNext)) {
                return null;
            } else {
                return new EntrySeq(iNext);
            }
        }
        @Override
        public Obj withMeta(IPersistentMap meta) {
            return new EntrySeq(i, meta);
        }
    }

    @Override
    public ISeq seq() {
        if (count == 0) {
            return null;
        } else {
            return new EntrySeq(0);
        }
    }

    private Object getValueByIndexUnlocked(final int i) {

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
        final int oid = col.typeOid();

        final IProcessor typeProcessor = Processors.getProcessor(oid);

//        final IProcessor typeProcessor = codecParams.getProcessor(oid);

        final Object value = switch (col.format()) {
            case TXT -> {
                final String string = new String(payload, offset, length, codecParams.serverCharset());
                yield typeProcessor.decodeTxt(string, codecParams);
            }
            case BIN -> {
                final ByteBuffer buf = ByteBuffer.wrap(payload, offset, length);
                yield typeProcessor.decodeBin(buf, codecParams);
            }
        };

        parsedKeys[i] = true;
        parsedValues[i] = value;

        return value;
    }

    @SuppressWarnings("unused") // pg.fold
    public IPersistentVector keys () {
        return PersistentVector.create(keys);
    }

    @SuppressWarnings("unused") // pg.fold
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
        return count;
    }

    @Override
    public IPersistentCollection empty() {
        return PersistentHashMap.EMPTY;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Iterator iterator() {
        return toClojureMap().iterator();
    }

    @Override
    public Object nth(final int i) {
        if (missesIndex(i)) {
            throw new IndexOutOfBoundsException(String.format("the row map misses index %s", i));
        }
        return getValueByIndex(i);
    }

    @Override
    public Object nth(final int i, final Object notFound) {
        if (missesIndex(i)) {
            return notFound;
        }
        return getValueByIndex(i);
    }
}
