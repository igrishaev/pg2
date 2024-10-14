package org.pg.type.processor;

import clojure.lang.IPersistentMap;
import clojure.lang.Symbol;
import org.pg.codec.*;
import org.pg.enums.OID;
import org.pg.json.JSON;
import org.pg.type.PGEnum;
import org.pg.util.BBTool;
import org.pg.util.DateTool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.Date;
import java.util.UUID;

public class Default extends AProcessor {

    public static final int oid = OID.DEFAULT;

    // TODO: remove this
    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return PrimitiveBin.encodeString(s, codecParams);
        } else if (x instanceof Short s) {
            return PrimitiveBin.encodeShort(s);
        } else if (x instanceof Integer i) {
            return BBTool.ofInt(i);
        } else if (x instanceof Long l) {
            return BBTool.ofLong(l);
        } else if (x instanceof Float f) {
            return BBTool.ofFloat(f);
        } else if (x instanceof Double d) {
            return BBTool.ofDouble(d);
        } else if (x instanceof BigDecimal bd) {
            return NumericBin.encode(bd);
        } else if (x instanceof BigInteger bi) {
            return NumericBin.encode(new BigDecimal(bi));
        } else if (x instanceof Boolean b) {
            return BBTool.ofBool(b);
        } else if (x instanceof UUID u) {
            return BBTool.ofUUID(u);
        } else if (x instanceof byte[] ba) {
            return ByteBuffer.wrap(ba);
        } else if (x instanceof PGEnum e) {
            return PrimitiveBin.encodeString(e.x(), codecParams);
        } else if (x instanceof JSON.Wrapper w) {
            return JsonBin.encodeJSONB(w.value(), codecParams);
        } else if (x instanceof ByteBuffer bb) {
            return bb;
        } else if (x instanceof Date d) {
            return DateTimeBin.encodeDATE(DateTool.toLocalDate(d));
        } else if (x instanceof Instant i) {
            return DateTimeBin.encodeTIMESTAMPTZ(i);
        } else if (x instanceof IPersistentMap pm) {
            return JsonBin.encodeJSONB(pm, codecParams);
        } else if (x instanceof LocalTime lt) {
            return DateTimeBin.encodeTIME(lt);
        } else if (x instanceof OffsetTime ot) {
            return DateTimeBin.encodeTIMETZ(ot);
        } else if (x instanceof LocalDate ld) {
            return DateTimeBin.encodeDATE(ld);
        } else if (x instanceof LocalDateTime ldt) {
            return DateTimeBin.encodeTIMESTAMP(DateTool.toInstant(ldt));
        } else if (x instanceof OffsetDateTime odt) {
            return DateTimeBin.encodeTIMESTAMPTZ(odt);
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeBin.encodeTIMESTAMPTZ(zdt);
        } else if (x instanceof Byte b) {
            return BBTool.ofShort(b.shortValue());
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Boolean b) {
            return PrimitiveTxt.encodeBool(b);
        } else if (x instanceof String s) {
            return s;
        } else if (x instanceof Character c) {
            return c.toString();
        } else if (x instanceof Symbol s) {
            return s.toString();
        } else if (x instanceof PGEnum e) {
            return e.x();
        } else if (x instanceof UUID u) {
            return u.toString();
        } else if (x instanceof IPersistentMap) {
            return JsonTxt.encodeJson(x, codecParams);
        } else if (x instanceof JSON.Wrapper w) {
            return JsonTxt.encodeJson(w.value(), codecParams);
        } else if (x instanceof byte[] ba) {
            return PrimitiveTxt.encodeBytea(ba);
        } else if (x instanceof Number n) {
            return n.toString();
        } else if (x instanceof OffsetTime ot) {
            return DateTimeTxt.encodeTIMETZ(ot);
        } else if (x instanceof LocalTime lt) {
            return DateTimeTxt.encodeTIME(lt);
        } else if (x instanceof OffsetDateTime odt) {
            return DateTimeTxt.encodeTIMESTAMPTZ(odt);
        } else if (x instanceof LocalDateTime ldt) {
            return DateTimeTxt.encodeTIMESTAMP(ldt);
        } else if (x instanceof ZonedDateTime zdt) {
            return DateTimeTxt.encodeTIMESTAMPTZ(zdt);
        } else if (x instanceof LocalDate ld) {
            return DateTimeTxt.encodeDATE(ld);
        } else if (x instanceof Instant i) {
            return DateTimeTxt.encodeTIMESTAMPTZ(i);
        } else if (x instanceof Date d) {
            return DateTimeTxt.encodeTIMESTAMPTZ(DateTool.toInstant(d));
        } else {
            return txtEncodingError(x, oid);
        }
    }
}
