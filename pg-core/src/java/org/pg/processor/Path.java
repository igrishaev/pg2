package org.pg.processor;

import clojure.lang.IPersistentCollection;
import org.pg.codec.CodecParams;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.util.Map;

public class Path extends AProcessor {

    public static final int oid = OID.PATH;

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof org.pg.type.Path p) {
            return p.toByteBuffer();
        } else if (x instanceof Iterable<?> i) {
            return org.pg.type.Path.fromIterable(i).toByteBuffer();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Path.fromMap(m).toByteBuffer();
        } else if (x instanceof String s) {
            return org.pg.type.Path.fromSQL(s).toByteBuffer();
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof org.pg.type.Path p) {
            return p.toSQL();
        } else if (x instanceof Iterable<?> i) {
            return org.pg.type.Path.fromIterable(i).toSQL();
        } else if (x instanceof Map<?,?> m) {
            return org.pg.type.Path.fromMap(m).toSQL();
        } else if (x instanceof String s) {
            return org.pg.type.Path.fromSQL(s).toSQL();
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public IPersistentCollection decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return org.pg.type.Path.fromByteBuffer(bb).toClojure();
    }

    @Override
    public IPersistentCollection decodeTxt(final String sql, final CodecParams codecParams) {
        return org.pg.type.Path.fromSQL(sql).toClojure();
    }

}
