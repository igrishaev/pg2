package org.pg.codec;

import clojure.lang.IPersistentCollection;
import clojure.lang.Symbol;
import org.pg.error.PGError;
import org.pg.enums.OID;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Date;
import java.util.UUID;

import org.pg.type.PGEnum;
import org.pg.util.HexTool;
import org.pg.json.JSON;

public final class EncoderTxt {

    public static String encode(final Object x) {
        return encode(x, OID.DEFAULT, CodecParams.standard());
    }

    public static String encode(final Object x, final OID oid) {
        return encode(x, oid, CodecParams.standard());
    }

    public static String encode(final Object x, final CodecParams codecParams) {
        return encode(x, OID.DEFAULT, codecParams);
    }

    private static String txtEncodingError(final Object x, final OID oid) {
        throw new PGError(
                "cannot text-encode a value: %s, OID: %s, type: %s",
                x, oid, x.getClass().getCanonicalName());
    }

    public static String encodeByteArray(final byte[] ba) {
        return HexTool.formatHex(ba, "\\x");
    }

    public static String encodeBool(final boolean value) {
        return value ? "t" : "f";
    }

    public static String encode(final Object x, final OID oid, final CodecParams codecParams) {

        if (x == null) {
            throw new PGError("cannot text-encode a null value");
        }

        return switch (oid) {

            case DEFAULT -> {
                if (x instanceof Boolean b) {
                    yield encodeBool(b);
                } else if (x instanceof String s) {
                    yield s;
                } else if (x instanceof Character c) {
                    yield c.toString();
                } else if (x instanceof Symbol s) {
                    yield s.toString();
                } else if (x instanceof UUID u) {
                    yield u.toString();
                } else if (x instanceof IPersistentCollection) {
                    yield JSON.writeValueToString(codecParams.objectMapper(), x);
                } else if (x instanceof JSON.Wrapper w) {
                    yield JSON.writeValueToString(codecParams.objectMapper(), w.value());
                } else if (x instanceof byte[] ba) {
                    yield encodeByteArray(ba);
                } else if (x instanceof Number n) {
                    yield n.toString();
                } else if (x instanceof OffsetTime ot) {
                    yield DateTimeTxt.encodeTIMETZ(ot);
                } else if (x instanceof LocalTime lt) {
                    yield DateTimeTxt.encodeTIME(lt);
                } else if (x instanceof OffsetDateTime odt) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(odt);
                } else if (x instanceof LocalDateTime ldt) {
                    yield DateTimeTxt.encodeTIMESTAMP(ldt);
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(zdt);
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeTxt.encodeDATE(ld);
                } else if (x instanceof Instant i) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(i);
                } else if (x instanceof Date d) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(DT.toInstant(d));
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case BYTEA -> {
                if (x instanceof byte[] ba) {
                    yield encodeByteArray(ba);
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case NUMERIC -> {
                if (x instanceof Number n) {
                    yield n.toString();
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case UUID -> {
                if (x instanceof UUID u) {
                    yield u.toString();
                } else if (x instanceof String s) {
                    yield s;
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case BOOL -> {
                if (x instanceof Boolean b) {
                    yield encodeBool(b);
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case JSON, JSONB -> {
                if (x instanceof String s) {
                    yield s;
                } else if (x instanceof JSON.Wrapper w) {
                    yield JSON.writeValueToString(codecParams.objectMapper(), w.value());
                } else {
                    yield JSON.writeValueToString(codecParams.objectMapper(), x);
                }
            }

            case TEXT, CHAR, VARCHAR, BPCHAR, NAME -> {
                if (x instanceof String s) {
                    yield s;
                } else if (x instanceof Character c) {
                    yield c.toString();
                } else if (x instanceof Symbol s) {
                    yield s.toString();
                } else if (x instanceof PGEnum e) {
                    yield e.x();
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case INT2, INT4, INT8, FLOAT4, FLOAT8, OID -> {
                if (x instanceof Number n) {
                    yield n.toString();
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case TIME -> {
                if (x instanceof LocalTime lt) {
                    yield DateTimeTxt.encodeTIME(lt);
                } else if (x instanceof OffsetTime ot) {
                    yield DateTimeTxt.encodeTIME(DT.toLocalTime(ot));
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case TIMETZ -> {
                if (x instanceof LocalTime lt) {
                    yield DateTimeTxt.encodeTIMETZ(DT.toOffsetTime(lt));
                } else if (x instanceof OffsetTime ot) {
                    yield DateTimeTxt.encodeTIMETZ(ot);
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case TIMESTAMPTZ -> {
                if (x instanceof OffsetDateTime odt) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(odt);
                } else if (x instanceof LocalDateTime ldt) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(DT.toInstant(ldt));
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(zdt);
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(DT.toInstant(ld));
                } else if (x instanceof Instant i) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(i);
                } else if (x instanceof Date d) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(d.toInstant());
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case TIMESTAMP -> {
                if (x instanceof OffsetDateTime odt) {
                    yield DateTimeTxt.encodeTIMESTAMP(odt.toInstant());
                } else if (x instanceof LocalDateTime ldt) {
                    yield DateTimeTxt.encodeTIMESTAMP(DT.toInstant(ldt));
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeTxt.encodeTIMESTAMP(DT.toLocalDateTime(zdt));
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeTxt.encodeTIMESTAMP(DT.toInstant(ld));
                } else if (x instanceof Instant i) {
                    yield DateTimeTxt.encodeTIMESTAMP(i);
                } else if (x instanceof Date d) {
                    yield DateTimeTxt.encodeTIMESTAMP(DT.toInstant(d));
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case DATE -> {
                if (x instanceof OffsetDateTime odt) {
                    yield DateTimeTxt.encodeDATE(DT.toLocalDate(odt));
                } else if (x instanceof LocalDateTime ldt) {
                    yield DateTimeTxt.encodeDATE(DT.toLocalDate(ldt));
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeTxt.encodeDATE(DT.toLocalDate(zdt));
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeTxt.encodeDATE(ld);
                } else if (x instanceof Instant i) {
                    yield DateTimeTxt.encodeDATE(DT.toLocalDate(i));
                } else if (x instanceof Date d) {
                    yield DateTimeTxt.encodeDATE(DT.toLocalDate(d));
                } else {
                    yield txtEncodingError(x, oid);
                }

            }

            default -> txtEncodingError(x, oid);
        };
    }

    public static void main (final String[] args) {
        System.out.println(encode("hello".getBytes(StandardCharsets.UTF_8)));
    }
}
