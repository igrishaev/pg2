package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.util.DateTool;
import org.pg.codec.DateTimeBin;
import org.pg.codec.DateTimeTxt;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.time.*;

public class Timetz extends AProcessor {

    public static final int oid = OID.TIMETZ;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof OffsetTime ot) {
            return DateTimeBin.encodeTIMETZ(ot);
        } else if (x instanceof LocalTime lt) {
            return DateTimeBin.encodeTIMETZ(DateTool.toOffsetTime(lt));
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof LocalTime lt) {
            return DateTimeTxt.encodeTIMETZ(DateTool.toOffsetTime(lt));
        } else if (x instanceof OffsetTime ot) {
            return DateTimeTxt.encodeTIMETZ(ot);
        } else if (x instanceof String s) {
            return s;
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public OffsetTime decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return DateTimeBin.decodeTIMETZ(bb);
    }

    @Override
    public OffsetTime decodeTxt(final String string, final CodecParams codecParams) {
        return DateTimeTxt.decodeTIMETZ(string);
    }
}
