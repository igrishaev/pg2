package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.codec.DT;
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
            return DateTimeBin.encodeDATE(DT.toLocalDate(d));
        } else if (x instanceof OffsetDateTime odt) {
            return DateTimeBin.encodeDATE(DT.toLocalDate(odt));
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeBin.encodeDATE(DT.toLocalDate(zdt));
        } else if (x instanceof Instant i) {
            return DateTimeBin.encodeDATE(DT.toLocalDate(i));
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof OffsetDateTime odt) {
            return DateTimeTxt.encodeDATE(DT.toLocalDate(odt));
        } else if (x instanceof LocalDateTime ldt) {
            return DateTimeTxt.encodeDATE(DT.toLocalDate(ldt));
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeTxt.encodeDATE(DT.toLocalDate(zdt));
        } else if (x instanceof LocalDate ld) {
            return DateTimeTxt.encodeDATE(ld);
        } else if (x instanceof Instant i) {
            return DateTimeTxt.encodeDATE(DT.toLocalDate(i));
        } else if (x instanceof java.util.Date d) {
            return DateTimeTxt.encodeDATE(DT.toLocalDate(d));
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
