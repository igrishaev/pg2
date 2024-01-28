package org.pg.clojure;

import clojure.lang.*;
import org.pg.codec.CodecParams;
import org.pg.codec.DecoderBin;
import org.pg.codec.DecoderTxt;
import org.pg.msg.DataRow;
import org.pg.msg.RowDescription;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.util.*;

public final class LazyMap extends APersistentMap {

    private final DataRow dataRow;
    private final RowDescription rowDescription;
    private final Object[] keys;
    private final CodecParams codecParams;
    private final Map<Object, Object> parsedValues;
    private final Map<Object, Short> keysIndex;

    public LazyMap(final DataRow dataRow,
                   final RowDescription rowDescription,
                   final Object[] keys,
                   final Map<Object, Short> keysIndex,
                   final CodecParams codecParams
    ) {
        this.dataRow = dataRow;
        this.rowDescription = rowDescription;
        this.keys = keys;
        this.keysIndex = keysIndex;
        this.codecParams = codecParams;
        this.parsedValues = new HashMap<>(keys.length);
    }

    private IPersistentMap toMap () {
        IPersistentMap result = PersistentHashMap.EMPTY;
        for (final Object key: keys) {
            result = result.assoc(key, getValueByKey(key));
        }
        return result;
    }

    private Object getValueByKey (final Object key) {

        if (!keysIndex.containsKey(key)) {
            return null;
        }

        if (parsedValues.containsKey(key)) {
            return parsedValues.get(key);
        }

        final int i = keysIndex.get(key);
        final ByteBuffer buf = dataRow.values()[i];

        if (buf == null) {
            parsedValues.put(key, null);
            return null;
        }

        final RowDescription.Column col = rowDescription.columns()[i];

        final Object value = switch (col.format()) {
            case TXT -> {
                final String string = BBTool.getString(buf, codecParams.serverCharset);
                yield DecoderTxt.decode(string, col.typeOid());
            }
            case BIN -> DecoderBin.decode(buf, col.typeOid(), codecParams);
        };

        parsedValues.put(key, value);
        return value;
    }

    @Override
    public String toString () {
        return String.format("<LazyMap, keys: %s, evaluated values: %s>",
                Arrays.toString(keys),
                parsedValues
        );
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
        return toMap().assoc(key, val);
    }

    @Override
    public IPersistentMap assocEx(final Object key, final Object val) {
        return toMap().assocEx(key, val);
    }

    @Override
    public IPersistentMap without(final Object key) {
        return toMap().without(key);
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
        return keys.length;
    }

    @Override
    public IPersistentCollection empty() {
        return PersistentHashMap.EMPTY;
    }

    @Override
    public ISeq seq() {
        return toMap().seq();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Iterator iterator() {
        return toMap().iterator();
    }

}
