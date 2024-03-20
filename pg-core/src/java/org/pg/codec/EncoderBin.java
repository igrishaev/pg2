package org.pg.codec;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;
import java.math.BigInteger;
import clojure.lang.BigInt;

import org.pg.type.PGEnum;
import org.pg.Const;
import org.pg.error.PGError;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.json.JSON;

public final class EncoderBin {

    public static ByteBuffer encode (Object x) {
        return encode(x, OID.DEFAULT, CodecParams.standard());
    }

    public static ByteBuffer encode (Object x, CodecParams codecParams) {
        return encode(x, OID.DEFAULT, codecParams);
    }

    public static ByteBuffer encode (Object x, OID oid) {
        return encode(x, oid, CodecParams.standard());
    }

    private static ByteBuffer binEncodingError(Object x, OID oid) {
        throw new PGError(
                "cannot binary-encode a value: %s, OID: %s, type: %s",
                x, oid, x.getClass().getCanonicalName()
        );
    }

    private static byte[] getBytes (String string, CodecParams codecParams) {
        return string.getBytes(codecParams.clientCharset());
    }

    public static ByteBuffer encode (Object x, OID oid, CodecParams codecParams) {

        if (x == null) {
            throw new PGError("cannot binary-encode a null value");
        }

        return switch (x.getClass().getCanonicalName()) {

            case "clojure.lang.Symbol" -> switch (oid) {
                case TEXT, VARCHAR, DEFAULT -> {
                    byte[] bytes = getBytes(x.toString(), codecParams);
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case "byte[]" -> switch (oid) {
                case BYTEA, DEFAULT -> ByteBuffer.wrap((byte[])x);
                default -> binEncodingError(x, oid);
            };

            case "java.lang.String" -> switch (oid) {
                case TEXT, VARCHAR, NAME, JSON, DEFAULT -> {
                    byte[] bytes = getBytes((String)x, codecParams);
                    yield ByteBuffer.wrap(bytes);
                }
                case JSONB -> {
                    byte[] bytes = getBytes((String)x, codecParams);
                    final ByteBuffer buf = ByteBuffer.allocate(bytes.length + 1);
                    buf.put(Const.JSONB_VERSION);
                    buf.put(bytes);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case "org.pg.type.PGEnum" -> switch (oid) {
                case DEFAULT, TEXT, VARCHAR -> {
                    byte[] bytes = getBytes(((PGEnum)x).x(), codecParams);
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case "java.lang.Character" -> switch (oid) {
                case TEXT, VARCHAR, CHAR, BPCHAR, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(2);
                    buf.put(x.toString().getBytes(codecParams.clientCharset()));
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case "java.lang.Short" -> switch (oid) {
                case INT2, DEFAULT -> BBTool.ofShort((short)x);
                case INT4, OID -> BBTool.ofInt((short)x);
                case INT8 -> BBTool.ofLong((short)x);
                case FLOAT4 -> BBTool.ofFloat((short)x);
                case FLOAT8 -> BBTool.ofDouble((short)x);
                default -> binEncodingError(x, oid);
            };

            case "java.lang.Integer" -> switch (oid) {
                case INT2 -> BBTool.ofShort(((Integer)x).shortValue());
                case INT4, OID, DEFAULT -> BBTool.ofInt((int)x);
                case INT8 -> BBTool.ofLong((int)x);
                case FLOAT4 -> BBTool.ofFloat((int)x);
                case FLOAT8 -> BBTool.ofDouble((int)x);
                default -> binEncodingError(x, oid);
            };

            case "java.lang.Long" -> switch (oid) {
                case INT2 -> BBTool.ofShort(((Long)x).shortValue());
                case INT4, OID -> BBTool.ofInt(((Long)x).intValue());
                case INT8, DEFAULT -> BBTool.ofLong((long)x);
                case FLOAT4 -> BBTool.ofFloat((long)x);
                case FLOAT8 -> BBTool.ofDouble((long)x);
                default -> binEncodingError(x, oid);
            };

            case "java.lang.Byte" -> switch (oid) {
                case INT2, DEFAULT -> BBTool.ofShort((byte)x);
                case INT4 -> BBTool.ofInt((byte)x);
                case INT8 -> BBTool.ofLong((byte)x);
                default -> binEncodingError(x, oid);
            };

            case "java.lang.Boolean" -> switch (oid) {
                case BOOL, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(1);
                    buf.put((boolean)x ? (byte)1 : (byte)0);
                    yield buf;
                }
                default -> binEncodingError(x, oid);
            };

            case "java.util.UUID" -> switch (oid) {
                case UUID, DEFAULT -> {
                    ByteBuffer buf = ByteBuffer.allocate(16);
                    buf.putLong(((UUID)x).getMostSignificantBits());
                    buf.putLong(((UUID)x).getLeastSignificantBits());
                    yield buf;
                }
                case TEXT, VARCHAR -> {
                    byte[] bytes = getBytes(x.toString(), codecParams);
                    yield ByteBuffer.wrap(bytes);
                }
                default -> binEncodingError(x, oid);
            };

            case "java.lang.Float" -> switch (oid) {
                case FLOAT4, DEFAULT -> BBTool.ofFloat((float)x);
                case FLOAT8 -> BBTool.ofDouble((float)x);
                default -> binEncodingError(x, oid);
            };

            case "java.lang.Double" -> switch (oid) {
                case FLOAT4 -> {
                    float f = (float)x;

                    if (Float.isInfinite(f)) {
                        throw new PGError("double->float coercion led to an infinite value: %s", x);
                    }
                    if (Float.isNaN(f)) {
                        throw new PGError("double->float coercion led to a NAN value: %s", x);
                    }

                    yield BBTool.ofFloat(f);
                }
                case FLOAT8, DEFAULT -> BBTool.ofDouble((double)x);
                default -> binEncodingError(x, oid);
            };

            case "org.pg.json.JSON.Wrapper" -> switch (oid) {
                case JSON, JSONB, DEFAULT -> {
                    // TODO; guess the size?
                    ByteArrayOutputStream out = new ByteArrayOutputStream(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(codecParams.objectMapper(), out, ((JSON.Wrapper)x).value());
                    yield ByteBuffer.wrap(out.toByteArray());
                }
                default -> binEncodingError(x, oid);
            };

            case
                    "clojure.lang.PersistentArrayMap",
                    "clojure.lang.PersistentHashMap",
                    "clojure.lang.PersistentHashSet",
                    "clojure.lang.PersistentList",
                    "clojure.lang.PersistentVector" -> switch (oid) {
                case JSON, JSONB, DEFAULT -> {
                    // TODO; guess the size?
                    ByteArrayOutputStream out = new ByteArrayOutputStream(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(codecParams.objectMapper(), out, x);
                    yield ByteBuffer.wrap(out.toByteArray());
                }
                default -> binEncodingError(x, oid);
            };

            case "java.util.Date" -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(LocalDate.ofInstant(((Date)x).toInstant(), ZoneOffset.UTC));
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP(((Date)x).toInstant());
                case TIMESTAMPTZ, DEFAULT -> DateTimeBin.encodeTIMESTAMPTZ(((Date)x).toInstant());
                default -> binEncodingError(x, oid);
            };

            case "java.time.OffsetTime" -> switch (oid) {
                case TIME -> DateTimeBin.encodeTIME(((OffsetTime)x).toLocalTime());
                case TIMETZ, DEFAULT -> DateTimeBin.encodeTIMETZ((OffsetTime)x);
                default -> binEncodingError(x, oid);
            };

            case "java.time.LocalTime" -> switch (oid) {
                case TIME, DEFAULT -> DateTimeBin.encodeTIME((LocalTime)x);
                case TIMETZ -> DateTimeBin.encodeTIMETZ(((LocalTime)x).atOffset(ZoneOffset.UTC));
                default -> binEncodingError(x, oid);
            };

            case "java.time.LocalDate" -> switch (oid) {
                case DATE, DEFAULT -> DateTimeBin.encodeDATE((LocalDate)x);
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP(
                        ((LocalDate)x).atStartOfDay(ZoneOffset.UTC).toInstant()
                );
                case TIMESTAMPTZ -> DateTimeBin.encodeTIMESTAMPTZ(
                        ((LocalDate)x).atStartOfDay(ZoneOffset.UTC).toInstant()
                );
                default -> binEncodingError(x, oid);
            };

            case "java.time.LocalDateTime" -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(((LocalDateTime)x).toLocalDate());
                case TIMESTAMP, DEFAULT -> DateTimeBin.encodeTIMESTAMP(((LocalDateTime)x).toInstant(ZoneOffset.UTC));
                case TIMESTAMPTZ -> DateTimeBin.encodeTIMESTAMPTZ(((LocalDateTime)x).toInstant(ZoneOffset.UTC));
                default -> binEncodingError(x, oid);
            };

            case "java.time.ZonedDateTime" -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(((ZonedDateTime)x).toLocalDate());
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP((ZonedDateTime)x);
                case TIMESTAMPTZ, DEFAULT -> DateTimeBin.encodeTIMESTAMPTZ((ZonedDateTime)x);
                default -> binEncodingError(x, oid);
            };

            case "java.time.OffsetDateTime" -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(((OffsetDateTime)x).toLocalDate());
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP((OffsetDateTime)x);
                case TIMESTAMPTZ, DEFAULT -> DateTimeBin.encodeTIMESTAMPTZ((OffsetDateTime)x);
                default -> binEncodingError(x, oid);
            };

            case "java.time.Instant" -> switch (oid) {
                case DATE -> DateTimeBin.encodeDATE(LocalDate.ofInstant((Instant)x, ZoneOffset.UTC));
                case TIMESTAMP -> DateTimeBin.encodeTIMESTAMP((Instant)x);
                case TIMESTAMPTZ, DEFAULT -> DateTimeBin.encodeTIMESTAMPTZ((Instant)x);
                default -> binEncodingError(x, oid);
            };

            case "java.math.BigDecimal" -> switch (oid) {
                case NUMERIC, DEFAULT -> NumericBin.encode((BigDecimal)x);
                case INT2 -> BBTool.ofShort(((BigDecimal)x).shortValueExact());
                case INT4 -> BBTool.ofInt(((BigDecimal)x).intValueExact());
                case INT8 -> BBTool.ofLong(((BigDecimal)x).longValueExact());
                case FLOAT4 -> BBTool.ofFloat(((BigDecimal)x).floatValue());
                case FLOAT8 -> BBTool.ofDouble(((BigDecimal)x).doubleValue());
                default -> binEncodingError(x, oid);
            };

            case "java.math.BigInteger" -> switch (oid) {
                case NUMERIC, DEFAULT -> NumericBin.encode(new BigDecimal((BigInteger)x));
                case INT2 -> BBTool.ofShort(((BigInteger)x).shortValueExact());
                case INT4 -> BBTool.ofInt(((BigInteger)x).intValueExact());
                case INT8 -> BBTool.ofLong(((BigInteger)x).longValueExact());
                case FLOAT4 -> BBTool.ofFloat(((BigInteger)x).floatValue());
                case FLOAT8 -> BBTool.ofDouble(((BigInteger)x).doubleValue());
                default -> binEncodingError(x, oid);
            };

            case "clojure.lang.BigInt" -> switch (oid) {
                case NUMERIC, DEFAULT -> NumericBin.encode(((BigInt)x).toBigDecimal());
                case INT2 -> BBTool.ofShort(((BigInt)x).shortValue());
                case INT4 -> BBTool.ofInt(((BigInt)x).intValue());
                case INT8 -> BBTool.ofLong(((BigInt)x).longValue());
                case FLOAT4 -> BBTool.ofFloat(((BigInt)x).floatValue());
                case FLOAT8 -> BBTool.ofDouble(((BigInt)x).doubleValue());
                default -> binEncodingError(x, oid);
            };

            default -> binEncodingError(x, oid);
        };
    }
}
