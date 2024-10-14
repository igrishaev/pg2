package org.pg.type.processor;

import clojure.lang.BigInt;
import org.pg.codec.CodecParams;
import org.pg.codec.NumericBin;
import org.pg.enums.OID;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

public class Numeric extends AProcessor {

    public static final int oid = OID.NUMERIC;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof BigDecimal bd) {
            return NumericBin.encode(bd);
        } else if (x instanceof BigInteger bi) {
            return NumericBin.encode(new BigDecimal(bi));
        } else if (x instanceof BigInt bi) {
            return NumericBin.encode(bi.toBigDecimal());
        } else if (x instanceof Long l) {
            return NumericBin.encode(new BigDecimal(l));
        } else if (x instanceof Integer i) {
            return NumericBin.encode(new BigDecimal(i));
        } else if (x instanceof Short s) {
            return NumericBin.encode(new BigDecimal(s));
        } else if (x instanceof Float f) {
            return NumericBin.encode(new BigDecimal(f));
        } else if (x instanceof Double d) {
            return NumericBin.encode(new BigDecimal(d));
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Number n) {
            return n.toString();
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public BigDecimal decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return NumericBin.decode(bb);
    }

    @Override
    public BigDecimal decodeTxt(final String text, final CodecParams codecParams) {
        return new BigDecimal(text);
    }
}
