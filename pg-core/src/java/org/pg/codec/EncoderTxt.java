package org.pg.codec;

import clojure.lang.IPersistentCollection;
import clojure.lang.Symbol;
import org.pg.Const;
import org.pg.error.PGError;
import org.pg.enums.OID;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Date;
import java.io.StringWriter;

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

    public static LocalDate toLocalDate(final Date date) {
        return LocalDate.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public static LocalDate toLocalDate(final Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }

    public static Instant toInstant(final LocalDate localDate) {
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant toInstant(final LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    public static String encode(final Object x, final OID oid, final CodecParams codecParams) {

        if (x == null) {
            throw new PGError("cannot text-encode a null value");
        }

        return switch (oid) {

            case DEFAULT -> {
                if (x instanceof Boolean b) {
                    yield b ? "t" : "f";
                } else if (x instanceof IPersistentCollection) {
                    yield JSON.writeValueToString(codecParams.objectMapper(), x);
                } else if (x instanceof byte[] ba) {
                    yield encodeByteArray(ba);
                } else {
                    yield x.toString();
                }
            }

            case BYTEA -> {
                if (x instanceof byte[] ba) {
                    yield encodeByteArray(ba);
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case JSON, JSONB -> {
                if (x instanceof IPersistentCollection) {
                    yield JSON.writeValueToString(codecParams.objectMapper(), x);
                } else {
                    yield txtEncodingError(x, oid);
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
                    yield DateTimeTxt.encodeTIME(ot.toLocalTime());
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case TIMETZ -> {
                if (x instanceof LocalTime lt) {
                    yield DateTimeTxt.encodeTIMETZ(lt.atOffset(ZoneOffset.UTC));
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
                    yield DateTimeTxt.encodeTIMESTAMPTZ(ldt.toInstant(ZoneOffset.UTC));
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(zdt);
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(ld.atStartOfDay(ZoneOffset.UTC).toInstant());
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
                    yield DateTimeTxt.encodeTIMESTAMP(ldt.toInstant(ZoneOffset.UTC));
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeTxt.encodeTIMESTAMP(zdt.toLocalDateTime());
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeTxt.encodeTIMESTAMP(toInstant(ld));
                } else if (x instanceof Instant i) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(i);
                } else if (x instanceof Date d) {
                    yield DateTimeTxt.encodeTIMESTAMPTZ(d.toInstant());
                } else {
                    yield txtEncodingError(x, oid);
                }
            }

            case DATE -> {
                if (x instanceof OffsetDateTime odt) {
                    yield DateTimeTxt.encodeDATE(odt.toLocalDate());
                } else if (x instanceof LocalDateTime ldt) {
                    yield DateTimeTxt.encodeDATE(ldt.toLocalDate());
                } else if (x instanceof ZonedDateTime zdt) {
                    yield DateTimeTxt.encodeDATE(zdt.toLocalDate());
                } else if (x instanceof LocalDate ld) {
                    yield DateTimeTxt.encodeDATE(ld);
                } else if (x instanceof Instant i) {
                    yield DateTimeTxt.encodeDATE(toLocalDate(i));
                } else if (x instanceof Date d) {
                    yield DateTimeTxt.encodeDATE(toLocalDate(d));
                } else {
                    yield txtEncodingError(x, oid);
                }

            }

            default -> txtEncodingError(x, oid);
        };

//        return switch (x.getClass().getCanonicalName()) {
//
//            case "clojure.lang.Symbol" -> switch (oid) {
//                case TEXT, VARCHAR, DEFAULT -> x.toString();
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.lang.Character" -> switch (oid) {
//                case TEXT, VARCHAR, CHAR, BPCHAR, DEFAULT -> x.toString();
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.lang.String" -> switch (oid) {
//                case TEXT, VARCHAR, NAME, JSON, JSONB, DEFAULT -> (String)x;
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "org.pg.type.PGEnum" -> switch (oid) {
//                case DEFAULT, TEXT, VARCHAR -> ((PGEnum)x).x();
//                default -> txtEncodingError(x, oid);
//            };
//
//            case
//                    "java.lang.Short",
//                    "java.lang.Integer",
//                    "java.lang.Long" -> switch (oid) {
//                case INT2, INT4, INT8, OID, NUMERIC, FLOAT4, FLOAT8, DEFAULT -> x.toString();
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "byte[]" -> switch (oid) {
//                case BYTEA, DEFAULT -> HexTool.formatHex((byte[])x, "\\x");
//                default -> txtEncodingError(x, oid);
//            };
//
//
//            case
//                    "java.lang.Float",
//                    "java.lang.Double"-> switch (oid) {
//                case FLOAT4, FLOAT8, DEFAULT -> x.toString();
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.util.UUID" -> switch (oid) {
//                case UUID, TEXT, VARCHAR, DEFAULT -> x.toString();
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.lang.Boolean" -> switch (oid) {
//                case BOOL, DEFAULT -> (boolean)x ? "t" : "f";
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.math.BigDecimal" -> switch (oid) {
//                case NUMERIC, FLOAT4, FLOAT8, DEFAULT -> x.toString();
//                default -> txtEncodingError(x, oid);
//            };
//
//            case
//                    "java.math.BigInteger",
//                    "clojure.lang.BigInt" -> switch (oid) {
//                case INT2, INT4, INT8, FLOAT4, FLOAT8, NUMERIC, DEFAULT -> x.toString();
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "org.pg.json.JSON.Wrapper" -> switch (oid) {
//                case JSON, JSONB, DEFAULT -> {
//                    // TODO: maybe return bytes?
//                    // TODO: guess the initial size?
//                    final StringWriter writer = new StringWriter(Const.JSON_ENC_BUF_SIZE);
//                    JSON.writeValue(codecParams.objectMapper(), writer, ((JSON.Wrapper)x).value());
//                    yield writer.toString();
//                }
//                default -> txtEncodingError(x, oid);
//            };
//
//            case
//                    "clojure.lang.PersistentArrayMap",
//                    "clojure.lang.PersistentHashMap",
//                    "clojure.lang.PersistentHashSet",
//                    "clojure.lang.PersistentList",
//                    "clojure.lang.PersistentVector" -> switch (oid) {
//                // TODO: maybe return bytes?
//                // TODO: guess the initial size?
//                case JSON, JSONB, DEFAULT -> {
//                    final StringWriter writer = new StringWriter(Const.JSON_ENC_BUF_SIZE);
//                    JSON.writeValue(codecParams.objectMapper(), writer, x);
//                    yield writer.toString();
//                }
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.util.Date" -> switch (oid) {
//                case DATE -> DateTimeTxt.encodeDATE(LocalDate.ofInstant(((Date)x).toInstant(), ZoneOffset.UTC));
//                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(((Date)x).toInstant());
//                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(((Date)x).toInstant());
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.time.OffsetTime" -> switch (oid) {
//                case TIME -> DateTimeTxt.encodeTIME(((OffsetTime)x).toLocalTime());
//                case TIMETZ, DEFAULT -> DateTimeTxt.encodeTIMETZ(((OffsetTime)x));
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.time.LocalTime" -> switch (oid) {
//                case TIME, DEFAULT -> DateTimeTxt.encodeTIME(((LocalTime)x));
//                case TIMETZ -> DateTimeTxt.encodeTIMETZ(((LocalTime)x).atOffset(ZoneOffset.UTC));
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.time.LocalDate" -> switch (oid) {
//                case DATE, DEFAULT -> DateTimeTxt.encodeDATE(((LocalDate)x));
//                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(
//                        ((LocalDate)x).atStartOfDay(ZoneOffset.UTC).toInstant()
//                );
//                case TIMESTAMPTZ -> DateTimeTxt.encodeTIMESTAMPTZ(
//                        ((LocalDate)x).atStartOfDay(ZoneOffset.UTC).toInstant()
//                );
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.time.LocalDateTime" -> switch (oid) {
//                case DATE -> DateTimeTxt.encodeDATE(((LocalDateTime)x).toLocalDate());
//                case TIMESTAMP, DEFAULT -> DateTimeTxt.encodeTIMESTAMP(((LocalDateTime)x).toInstant(ZoneOffset.UTC));
//                case TIMESTAMPTZ -> DateTimeTxt.encodeTIMESTAMPTZ(((LocalDateTime)x).toInstant(ZoneOffset.UTC));
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.time.ZonedDateTime" -> switch (oid) {
//                case DATE -> DateTimeTxt.encodeDATE(((ZonedDateTime)x).toLocalDate());
//                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(((ZonedDateTime)x));
//                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(((ZonedDateTime)x));
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.time.OffsetDateTime" -> switch (oid) {
//                case DATE -> DateTimeTxt.encodeDATE(((OffsetDateTime)x).toLocalDate());
//                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP(((OffsetDateTime)x));
//                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ(((OffsetDateTime)x));
//                default -> txtEncodingError(x, oid);
//            };
//
//            case "java.time.Instant" -> switch (oid) {
//                case DATE -> DateTimeTxt.encodeDATE(LocalDate.ofInstant((Instant)x, ZoneOffset.UTC));
//                case TIMESTAMP -> DateTimeTxt.encodeTIMESTAMP((Instant)x);
//                case TIMESTAMPTZ, DEFAULT -> DateTimeTxt.encodeTIMESTAMPTZ((Instant)x);
//                default -> txtEncodingError(x, oid);
//            };
//
//            default -> txtEncodingError(x, oid);
//        };
    }

    public static void main (final String[] args) {
        System.out.println(encode("hello".getBytes(StandardCharsets.UTF_8)));
    }
}
