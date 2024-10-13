package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.util.DateTool;
import org.pg.codec.DateTimeBin;
import org.pg.codec.DateTimeTxt;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.time.*;

public class Date extends AProcessor {

    public static final int oid = OID.DATE;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof LocalDate ld) {
            return DateTimeBin.encodeDATE(ld);
        } else if (x instanceof LocalDateTime ldt) {
            return DateTimeBin.encodeDATE(ldt.toLocalDate());
        } else if (x instanceof java.util.Date d) {
            return DateTimeBin.encodeDATE(DateTool.toLocalDate(d));
        } else if (x instanceof OffsetDateTime odt) {
            return DateTimeBin.encodeDATE(DateTool.toLocalDate(odt));
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeBin.encodeDATE(DateTool.toLocalDate(zdt));
        } else if (x instanceof Instant i) {
            return DateTimeBin.encodeDATE(DateTool.toLocalDate(i));
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof OffsetDateTime odt) {
            return DateTimeTxt.encodeDATE(DateTool.toLocalDate(odt));
        } else if (x instanceof LocalDateTime ldt) {
            return DateTimeTxt.encodeDATE(DateTool.toLocalDate(ldt));
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeTxt.encodeDATE(DateTool.toLocalDate(zdt));
        } else if (x instanceof LocalDate ld) {
            return DateTimeTxt.encodeDATE(ld);
        } else if (x instanceof Instant i) {
            return DateTimeTxt.encodeDATE(DateTool.toLocalDate(i));
        } else if (x instanceof java.util.Date d) {
            return DateTimeTxt.encodeDATE(DateTool.toLocalDate(d));
        } else if (x instanceof String s) {
            return s;
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public LocalDate decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return DateTimeBin.decodeDATE(bb);
    }

    @Override
    public LocalDate decodeTxt(final String text, final CodecParams codecParams) {
        return DateTimeTxt.decodeDATE(text);
    }
}
