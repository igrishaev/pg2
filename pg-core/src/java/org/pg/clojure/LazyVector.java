package org.pg.clojure;

import clojure.lang.*;
import org.pg.codec.CodecParams;
import org.pg.codec.DecoderBin;
import org.pg.codec.DecoderTxt;
import org.pg.msg.DataRow;
import org.pg.msg.RowDescription;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
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

    private Object getValueByIndex (final int i) {

        if (0 < i || i >= dataRow.valueCount()) {
            return null;
        }

        if (parsedValues.containsKey(i)) {
            return parsedValues.get(i);
        }

        final ByteBuffer buf = dataRow.values()[i];

        if (buf == null) {
            parsedValues.put(i, null);
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

        parsedValues.put(i, value);
        return value;
    }

    private PersistentVector toVector() {
        PersistentVector result = PersistentVector.EMPTY;
        for (int i = 0; i < dataRow.valueCount(); i++) {
            result = result.cons(getValueByIndex(i));
        }
        return result;
    }

    @Override
    public IPersistentVector assocN(final int i, final Object val) {
        return toVector().assocN(i, val);
    }

    @Override
    public int count() {
        return dataRow.valueCount();
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
        return getValueByIndex(i);
    }
}
