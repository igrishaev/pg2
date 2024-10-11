package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.codec.DT;
import org.pg.codec.DateTimeBin;
import org.pg.codec.DateTimeTxt;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.time.*;
import java.util.Date;

public class Timestamp extends AProcessor {

    public static final int oid = OID.TIMESTAMP;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof LocalDateTime ldt) {
            return DateTimeBin.encodeTIMESTAMP(DT.toInstant(ldt));
        } else if (x instanceof Instant i) {
            return DateTimeBin.encodeTIMESTAMP(i);
        } else if (x instanceof OffsetDateTime odt) {
            return DateTimeBin.encodeTIMESTAMP(odt);
        } else if (x instanceof Date d) {
            return DateTimeBin.encodeTIMESTAMP(DT.toInstant(d));
        } else if (x instanceof LocalDate ld) {
            return DateTimeBin.encodeTIMESTAMP(DT.toInstant(ld));
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeBin.encodeTIMESTAMP(DT.toInstant(zdt));
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof OffsetDateTime odt) {
            return DateTimeTxt.encodeTIMESTAMP(odt.toInstant());
        } else if (x instanceof LocalDateTime ldt) {
            return DateTimeTxt.encodeTIMESTAMP(DT.toInstant(ldt));
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeTxt.encodeTIMESTAMP(DT.toLocalDateTime(zdt));
        } else if (x instanceof LocalDate ld) {
            return DateTimeTxt.encodeTIMESTAMP(DT.toInstant(ld));
        } else if (x instanceof Instant i) {
            return DateTimeTxt.encodeTIMESTAMP(i);
        } else if (x instanceof Date d) {
            return DateTimeTxt.encodeTIMESTAMP(DT.toInstant(d));
        } else if (x instanceof String s) {
            return s;
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public LocalDateTime decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return DateTimeBin.decodeTIMESTAMP(bb);
    }

    @Override
    public LocalDateTime decodeTxt(final String text, final CodecParams codecParams) {
        return DateTimeTxt.decodeTIMESTAMP(text);
    }
}
