package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.util.DateTool;
import org.pg.codec.DateTimeBin;
import org.pg.codec.DateTimeTxt;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.time.*;
import java.util.Date;

public class Timestamptz extends AProcessor {

    public static final int oid = OID.TIMESTAMPTZ;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof OffsetDateTime odt) {
            return DateTimeBin.encodeTIMESTAMPTZ(odt);
        } else if (x instanceof Instant i) {
            return DateTimeBin.encodeTIMESTAMPTZ(i);
        } else if (x instanceof LocalDate ld) {
            return DateTimeBin.encodeTIMESTAMPTZ(DateTool.toInstant(ld));
        } else if (x instanceof LocalDateTime ldt) {
            return DateTimeBin.encodeTIMESTAMPTZ(DateTool.toInstant(ldt));
        } else if (x instanceof Date d) {
            return DateTimeBin.encodeTIMESTAMPTZ(DateTool.toInstant(d));
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeBin.encodeTIMESTAMPTZ(zdt);
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof OffsetDateTime odt) {
            return DateTimeTxt.encodeTIMESTAMPTZ(odt);
        } else if (x instanceof LocalDateTime ldt) {
            return DateTimeTxt.encodeTIMESTAMPTZ(DateTool.toInstant(ldt));
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeTxt.encodeTIMESTAMPTZ(zdt);
        } else if (x instanceof LocalDate ld) {
            return DateTimeTxt.encodeTIMESTAMPTZ(DateTool.toInstant(ld));
        } else if (x instanceof Instant i) {
            return DateTimeTxt.encodeTIMESTAMPTZ(i);
        } else if (x instanceof Date d) {
            return DateTimeTxt.encodeTIMESTAMPTZ(d.toInstant());
        } else if (x instanceof String s) {
            return s;
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public OffsetDateTime decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        return DateTimeBin.decodeTIMESTAMPTZ(bb);
    }

    @Override
    public OffsetDateTime decodeTxt(final String text, final CodecParams codecParams) {
        return DateTimeTxt.decodeTIMESTAMPTZ(text);
    }
}
