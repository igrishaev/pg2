package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.util.BBTool;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

public class Float8 extends AProcessor {

    public static final int oid = OID.FLOAT4;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Float f) {
            return BBTool.ofDouble(f.doubleValue());
        } else if (x instanceof Double d) {
            return BBTool.ofDouble(d);
        } else if (x instanceof Short s) {
            return BBTool.ofDouble(s.doubleValue());
        } else if (x instanceof Integer i) {
            return BBTool.ofDouble(i.doubleValue());
        }  else if (x instanceof Long l) {
            return BBTool.ofDouble(l.doubleValue());
        } else if (x instanceof BigDecimal bc) {
            return BBTool.ofDouble(bc.doubleValue());
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        return x.toString();
    }

    @Override
    public Double decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return bb.getDouble();
    }

    @Override
    public Double decodeTxt(final String text, final CodecParams codecParams) {
        return Double.parseDouble(text);
    }
}
