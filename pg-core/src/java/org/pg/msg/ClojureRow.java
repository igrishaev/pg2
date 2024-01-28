package org.pg.msg;

import clojure.lang.*;
import org.pg.PGError;
import org.pg.codec.CodecParams;
import org.pg.codec.DecoderBin;
import org.pg.codec.DecoderTxt;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ClojureRow implements IPersistentMap {

    private final DataRow dataRow;
    private final RowDescription rowDescription;
    private final Object[] keys;
    private final CodecParams codecParams;
    private final Map<Object, Object> parsedValues;
    private final Map<Object, Short> keysIndex;

    public ClojureRow (final DataRow dataRow,
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
        // TODO
        return String.format("<ClojureRow, keys: %s>", Arrays.toString(keys));
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
    public IPersistentCollection cons(Object o) {
        throw new PGError("cons is not implemented");
    }

    // TODO: return an empty map?
    @Override
    public IPersistentCollection empty() {
        return toMap().empty();
    }

    @Override
    public boolean equiv(final Object o) {
        return toMap().equiv(o);
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
