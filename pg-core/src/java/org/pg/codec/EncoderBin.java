package org.pg.codec;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;
import java.math.BigInteger;

import clojure.lang.*;

import org.pg.type.PGEnum;
import org.pg.Const;
import org.pg.error.PGError;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.json.JSON;

public final class EncoderBin {

    public static ByteBuffer encode (final Object x) {
        return encode(x, OID.DEFAULT, CodecParams.standard());
    }

    public static ByteBuffer encode (final Object x, final CodecParams codecParams) {
        return encode(x, OID.DEFAULT, codecParams);
    }

    public static ByteBuffer encode (final Object x, final int oid) {
        return encode(x, oid, CodecParams.standard());
    }

    private static ByteBuffer binEncodingError(final Object x, final int oid, final String hint) {
        final String message = String.format(
                "cannot binary-encode a value: %s, OID: %s, type: %s",
                x, oid, x.getClass().getCanonicalName()
        );
        throw new PGError(hint == null ? message : message + ", hint: " + hint);
    }

    private static ByteBuffer binEncodingError(final Object x, final int oid) {
        return binEncodingError(x, oid, null);
    }

    private static byte[] getBytes (final String string, final CodecParams codecParams) {
        return string.getBytes(codecParams.clientCharset());
    }

    private static ByteBuffer encodeJSON (final Object x, final CodecParams codecParams) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Const.JSON_ENC_BUF_SIZE);
        JSON.writeValue(codecParams.objectMapper(), out, x);
        final byte[] bytes = out.toByteArray();
        return ByteBuffer.wrap(bytes);
    }

    private static ByteBuffer encodeJSONB (final Object x, final CodecParams codecParams) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Const.JSON_ENC_BUF_SIZE);
        JSON.writeValue(codecParams.objectMapper(), out, x);
        final byte[] bytes = out.toByteArray();
        final ByteBuffer buf = ByteBuffer.allocate(bytes.length + 1);
        buf.put(Const.JSONB_VERSION);
        buf.put(bytes);
        return buf;
    }

    private static ByteBuffer stringToJSONB(final String x, final CodecParams codecParams) {
        final byte[] bytes = getBytes(x, codecParams);
        final ByteBuffer buf = ByteBuffer.allocate(bytes.length + 1);
        buf.put(Const.JSONB_VERSION);
        buf.put(bytes);
        return buf;
    }

    private static ByteBuffer stringToBytes(final String x, final CodecParams codecParams) {
        final byte[] bytes = getBytes(x, codecParams);
        return ByteBuffer.wrap(bytes);
    }

    public static ByteBuffer encode (final Object x, final int oid, final CodecParams codecParams) {

        if (x == null) {
            throw new PGError("cannot binary-encode a null value");
        }

        return switch (oid) {

            case OID.DEFAULT -> {
                if (x instanceof String s) {
                    yield stringToBytes(s, codecParams);
                } else if (x instanceof Short s) {
                    yield BBTool.ofShort(s);
                } else if (x instanceof Integer i) {
                    yield BBTool.ofInt(i);
                } else if (x instanceof Long l) {
                    yield BBTool.ofLong(l);
                } else if (x instanceof Float f) {
                    yield BBTool.ofFloat(f);
                } else if (x instanceof Double d) {
                    yield BBTool.ofDouble(d);
                } else if (x instanceof BigDecimal bd) {
                    yield NumericBin.encode(bd);
                } else if (x instanceof BigInteger bi) {
                    yield NumericBin.encode(new BigDecimal(bi));
                } else if (x instanceof Boolean b) {
                    yield BBTool.ofBool(b);
                } else if (x instanceof UUID u) {
                    yield BBTool.ofUUID(u);
                } else if (x instanceof byte[] ba) {
                    yield ByteBuffer.wrap(ba);
                } else if (x instanceof PGEnum e) {
                    yield stringToBytes(e.x(), codecParams);
                } else if (x instanceof JSON.Wrapper w) {
                    yield encodeJSONB(w.value(), codecParams);
                } else if (x instanceof ByteBuffer bb) {
                    yield bb;
                } else if (x instanceof Date d) {
                    yield DateTimeBin.encodeDATE(DT.toLocalDate(d));
                } else if (x instanceof Instant i) {
                    yield DateTimeBin.encodeTIMESTAMPTZ(i);
                } else if (x instanceof IPersistentMap pc) {
                    yield encodeJSONB(pc, codecParams);
                } else if (x instanceof LocalTime lt) {
                    yield DateTimeBin.encodeTIME(lt);
                } else if (x instanceof OffsetTime ot) {
                    yield DateTimeBin.encodeTIMETZ(ot);
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeBin.encodeDATE(ld);
                } else if (x instanceof LocalDateTime ldt) {
                    yield DateTimeBin.encodeTIMESTAMP(DT.toInstant(ldt));
                } else if (x instanceof OffsetDateTime odt) {
                    yield DateTimeBin.encodeTIMESTAMPTZ(odt);
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeBin.encodeTIMESTAMPTZ(zdt);
                } else if (x instanceof Byte b) {
                    yield BBTool.ofShort(b.shortValue());
                } else {
                    yield binEncodingError(x, oid);
                }
            }
            
            case OID.INT2 -> {
                if (x instanceof Short s) {
                    yield BBTool.ofShort(s);
                } else if (x instanceof Integer i) {
                    yield BBTool.ofShort(i.shortValue());
                } else if (x instanceof Long l) {
                    yield BBTool.ofShort(l.shortValue());
                } else if (x instanceof BigInteger bi) {
                    yield BBTool.ofShort(bi.shortValue());
                } else if (x instanceof BigDecimal bc) {
                    yield BBTool.ofShort(bc.shortValueExact());
                } else if (x instanceof BigInt bi) {
                    yield BBTool.ofShort(bi.shortValue());
                } else {
                    yield binEncodingError(x, oid);
                }
            }
            
            case OID.INT4, OID.OID -> {
                if (x instanceof Short s) {
                    yield BBTool.ofInt(s.intValue());
                } else if (x instanceof Integer i) {
                    yield BBTool.ofInt(i);
                } else if (x instanceof Long l) {
                    yield BBTool.ofInt(l.intValue());
                } else if (x instanceof BigInteger bi) {
                    yield BBTool.ofInt(bi.intValue());
                } else if (x instanceof Byte b) {
                    yield BBTool.ofInt(b.intValue());
                } else if (x instanceof BigDecimal bc) {
                    yield BBTool.ofInt(bc.intValueExact());
                } else if (x instanceof BigInt bi) {
                    yield BBTool.ofInt(bi.intValue());
                } else {
                    yield binEncodingError(x, oid);
                }

            }

            case OID.INT8 -> {
                if (x instanceof Short s) {
                    yield BBTool.ofLong(s.longValue());
                } else if (x instanceof Integer i) {
                    yield BBTool.ofLong(i.longValue());
                } else if (x instanceof Long l) {
                    yield BBTool.ofLong(l);
                } else if (x instanceof BigInteger bi) {
                    yield BBTool.ofLong(bi.longValue());
                } else if (x instanceof Byte b) {
                    yield BBTool.ofLong(b.longValue());
                } else if (x instanceof BigDecimal bc) {
                    yield BBTool.ofLong(bc.longValueExact());
                } else if (x instanceof BigInt bi) {
                    yield BBTool.ofLong(bi.longValue());
                } else {
                    yield binEncodingError(x, oid);
                }
            }
            
            case OID.BYTEA -> {
                if (x instanceof ByteBuffer bb) {
                    yield bb;
                } else if (x instanceof byte[] ba) {
                    yield ByteBuffer.wrap(ba);
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.TEXT, OID.VARCHAR, OID.CHAR, OID.BPCHAR, OID.NAME -> {
                if (x instanceof String s) {
                    yield stringToBytes(s, codecParams);
                } else if (x instanceof Character c) {
                    yield stringToBytes(c.toString(), codecParams);
                } else if (x instanceof PGEnum e) {
                    yield stringToBytes(e.x(), codecParams);
                } else if (x instanceof Symbol s) {
                    yield stringToBytes(s.toString(), codecParams);
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.JSON -> {
                if (x instanceof String s) {
                    yield stringToBytes(s, codecParams);
                } else if (x instanceof JSON.Wrapper w) {
                    yield encodeJSON(w.value(), codecParams);
                } else {
                    yield encodeJSON(x, codecParams);
                }
            }

            case OID.JSONB -> {
                if (x instanceof String s) {
                    yield stringToJSONB(s, codecParams);
                } else if (x instanceof JSON.Wrapper w) {
                    yield encodeJSONB(w.value(), codecParams);
                }
                else {
                    yield encodeJSONB(x, codecParams);
                }
            }

            case OID.FLOAT4 -> {
                if (x instanceof Float f) {
                    yield BBTool.ofFloat(f);
                } else if (x instanceof Double d) {
                    yield BBTool.ofFloat(d.floatValue());
                } else if (x instanceof Short s) {
                    yield BBTool.ofFloat(s.floatValue());
                } else if (x instanceof Integer i) {
                    yield BBTool.ofFloat(i.floatValue());
                }  else if (x instanceof Long l) {
                    yield BBTool.ofFloat(l.floatValue());
                } else if (x instanceof BigDecimal bc) {
                    yield BBTool.ofFloat(bc.floatValue());
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.FLOAT8 -> {
                if (x instanceof Float f) {
                    yield BBTool.ofDouble(f.doubleValue());
                } else if (x instanceof Double d) {
                    yield BBTool.ofDouble(d);
                } else if (x instanceof Short s) {
                    yield BBTool.ofDouble(s.doubleValue());
                } else if (x instanceof Integer i) {
                    yield BBTool.ofDouble(i.doubleValue());
                }  else if (x instanceof Long l) {
                    yield BBTool.ofDouble(l.doubleValue());
                } else if (x instanceof BigDecimal bc) {
                    yield BBTool.ofDouble(bc.doubleValue());
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.UUID -> {
                if (x instanceof UUID u) {
                    yield BBTool.ofUUID(u);
                } else if (x instanceof String s) {
                    yield BBTool.ofUUID(UUID.fromString(s));
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.BOOL -> {
                if (x instanceof Boolean b) {
                    yield BBTool.ofBool(b);
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.NUMERIC -> {
                if (x instanceof BigDecimal bd) {
                    yield NumericBin.encode(bd);
                } else if (x instanceof BigInteger bi) {
                    yield NumericBin.encode(new BigDecimal(bi));
                } else if (x instanceof BigInt bi) {
                    yield NumericBin.encode(bi.toBigDecimal());
                } else if (x instanceof Long l) {
                    yield NumericBin.encode(new BigDecimal(l));
                } else if (x instanceof Integer i) {
                    yield NumericBin.encode(new BigDecimal(i));
                } else if (x instanceof Short s) {
                    yield NumericBin.encode(new BigDecimal(s));
                } else if (x instanceof Float f) {
                    yield NumericBin.encode(new BigDecimal(f));
                } else if (x instanceof Double d) {
                    yield NumericBin.encode(new BigDecimal(d));
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.TIME -> {
                if (x instanceof LocalTime lt) {
                    yield DateTimeBin.encodeTIME(lt);
                } else if (x instanceof OffsetTime ot) {
                    yield DateTimeBin.encodeTIME(DT.toLocalTime(ot));
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.TIMETZ -> {
                if (x instanceof OffsetTime ot) {
                    yield DateTimeBin.encodeTIMETZ(ot);
                } else if (x instanceof LocalTime lt) {
                    yield DateTimeBin.encodeTIMETZ(DT.toOffsetTime(lt));
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.DATE -> {
                if (x instanceof LocalDate ld) {
                    yield DateTimeBin.encodeDATE(ld);
                } else if (x instanceof LocalDateTime ldt) {
                    yield DateTimeBin.encodeDATE(ldt.toLocalDate());
                } else if (x instanceof Date d) {
                    yield DateTimeBin.encodeDATE(DT.toLocalDate(d));
                } else if (x instanceof OffsetDateTime odt) {
                    yield DateTimeBin.encodeDATE(DT.toLocalDate(odt));
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeBin.encodeDATE(DT.toLocalDate(zdt));
                } else if (x instanceof Instant i) {
                    yield DateTimeBin.encodeDATE(DT.toLocalDate(i));
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.TIMESTAMP -> {
                if (x instanceof LocalDateTime ldt) {
                    yield DateTimeBin.encodeTIMESTAMP(DT.toInstant(ldt));
                } else if (x instanceof Instant i) {
                    yield DateTimeBin.encodeTIMESTAMP(i);
                } else if (x instanceof OffsetDateTime odt) {
                    yield DateTimeBin.encodeTIMESTAMP(odt);
                } else if (x instanceof Date d) {
                    yield DateTimeBin.encodeTIMESTAMP(DT.toInstant(d));
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeBin.encodeTIMESTAMP(DT.toInstant(ld));
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeBin.encodeTIMESTAMP(DT.toInstant(zdt));
                } else {
                    yield binEncodingError(x, oid);
                }
            }

            case OID.TIMESTAMPTZ -> {
                if (x instanceof OffsetDateTime odt) {
                    yield DateTimeBin.encodeTIMESTAMPTZ(odt);
                } else if (x instanceof Instant i) {
                    yield DateTimeBin.encodeTIMESTAMPTZ(i);
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeBin.encodeTIMESTAMPTZ(DT.toInstant(ld));
                } else if (x instanceof LocalDateTime ldt) {
                    yield DateTimeBin.encodeTIMESTAMPTZ(DT.toInstant(ldt));
                } else if (x instanceof Date d) {
                    yield DateTimeBin.encodeTIMESTAMPTZ(DT.toInstant(d));
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeBin.encodeTIMESTAMPTZ(zdt);
                } else {
                    yield binEncodingError(x, oid);
                }
            }

//            case OID._TEXT, OID._VARCHAR, OID._NAME, OID._INT2, OID._INT4, OID._INT8, OID._OID, OID._CHAR, OID._BPCHAR, OID._UUID,
//                    OID._FLOAT4, OID._FLOAT8, OID._BOOL, OID._JSON, OID._JSONB, OID._TIME, OID._TIMETZ, OID._DATE, OID._TIMESTAMP,
//                    OID._TIMESTAMPTZ, OID._NUMERIC -> {
//                if (x instanceof Indexed) {
//                    yield ArrayBin.encode(x, oid, codecParams);
//                } else {
//                    yield binEncodingError(x, oid);
//                }
//            }

            // unsupported type, pass value as a byte array or BB
            default -> {
                if (x instanceof String s) {
                    yield stringToBytes(s, codecParams);
                } else if (x instanceof PGEnum e) {
                    yield stringToBytes(e.x(), codecParams);
                } else if (x instanceof byte[] bytes) {
                    yield ByteBuffer.wrap(bytes);
                } else if (x instanceof ByteBuffer bb) {
                    yield bb;
                }
                else {
                    yield binEncodingError(x, oid, "try to pass this value as a byte array or ByteBuffer");
                }
            }
        };

    }
}
