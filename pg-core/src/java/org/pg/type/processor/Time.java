package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.util.DateTool;
import org.pg.codec.DateTimeBin;
import org.pg.codec.DateTimeTxt;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.time.*;

public class Time extends AProcessor {

    public static final int oid = OID.TIME;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof LocalTime lt) {
            return DateTimeBin.encodeTIME(lt);
        } else if (x instanceof OffsetTime ot) {
            return DateTimeBin.encodeTIME(DateTool.toLocalTime(ot));
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof LocalTime lt) {
            return DateTimeTxt.encodeTIME(lt);
        } else if (x instanceof OffsetTime ot) {
            return DateTimeTxt.encodeTIME(DateTool.toLocalTime(ot));
        } else if (x instanceof String s) {
            return s;
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public LocalTime decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return DateTimeBin.decodeTIME(bb);
    }

    @Override
    public LocalTime decodeTxt(final String text, final CodecParams codecParams) {
        return DateTimeTxt.decodeTIME(text);
    }
}
