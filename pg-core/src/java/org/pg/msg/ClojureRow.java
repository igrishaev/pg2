package org.pg.msg;

import clojure.lang.*;
import org.pg.codec.CodecParams;
import org.pg.codec.DecoderBin;
import org.pg.codec.DecoderTxt;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class ClojureRow implements clojure.lang.Associative, clojure.lang.IPersistentMap {

    private final DataRow dataRow;
    private final RowDescription rowDescription;
    private final Object[] keys;
    private final CodecParams codecParams;
    private final HashMap<Object, Object> parsedValues;

    public ClojureRow (final DataRow dataRow, final RowDescription rowDescription, final Object[] keys, final CodecParams codecParams) {
        this.dataRow = dataRow;
        this.rowDescription = rowDescription;
        this.keys = keys;
        this.codecParams = codecParams;
        this.parsedValues = new HashMap<>(keys.length);
    }

    private Object getValueByKey (final Object key) {
        if (parsedValues.containsKey(key)) {
            return parsedValues.get(key);
        }

        final int i = Arrays.asList(keys).indexOf(key);
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

    private IPersistentMap toMap () {
        IPersistentMap result = PersistentHashMap.EMPTY;
        for (final Object key: keys) {
            if (parsedValues.containsKey(key)) {
                result = result.assoc(key, parsedValues.get(key));
            }
            else {
                result = result.assoc(key, getValueByKey(key));
            }
        }
        return result;
    }

    @Override
    public boolean containsKey(Object key) {
        return Arrays.asList(keys).contains(key);
    }

    @Override
    public IMapEntry entryAt(Object key) {
        final int i = Arrays.asList(keys).indexOf(key);
        final ByteBuffer buf = dataRow.values()[i];

        if (buf == null) {
            return new MapEntry(key, null);
        }

        final RowDescription.Column col = rowDescription.columns()[i];

        final Object value = switch (col.format()) {
            case TXT -> {
                final String string = BBTool.getString(buf, codecParams.serverCharset);
                yield DecoderTxt.decode(string, col.typeOid());
            }
            case BIN -> DecoderBin.decode(buf, col.typeOid(), codecParams);
        };

        return new MapEntry(key, value);
    }

    @Override
    public IPersistentMap assoc(Object key, Object val) {
        return toMap().assoc(key, val);
    }

    @Override
    public IPersistentMap assocEx(Object key, Object val) {
        return toMap().assocEx(key, val);
    }

    @Override
    public IPersistentMap without(Object key) {
        return toMap().without(key);
    }

    @Override
    public Object valAt(Object key) {
        if (parsedValues.containsKey(key)) {
            return parsedValues.get(key);
        }

        final int i = Arrays.asList(keys).indexOf(key);
        final ByteBuffer buf = dataRow.values()[i];

        if (buf == null) {
            parsedValues.put(key, null);
            return null;
        }

        final RowDescription.Column col = rowDescription.columns()[i];

        return switch (col.format()) {
            case TXT -> {
                final String string = BBTool.getString(buf, codecParams.serverCharset);
                yield DecoderTxt.decode(string, col.typeOid());
            }
            case BIN -> DecoderBin.decode(buf, col.typeOid(), codecParams);
        };
    }

    @Override
    public String toString () {
        return toMap().toString();
    }

    @Override
    public Object valAt(Object key, Object notFound) {
        return null;
    }

    @Override
    public int count() {
        return keys.length;
    }

    @Override
    public IPersistentCollection cons(Object o) {
        return null;
    }

    @Override
    public IPersistentCollection empty() {
        return null;
    }

    @Override
    public boolean equiv(Object o) {
        return false;
    }

    @Override
    public ISeq seq() {
        return null;
    }

    @Override
    public Iterator<Object> iterator() {
        return null;
    }
}
