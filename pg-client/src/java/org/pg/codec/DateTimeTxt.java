package org.pg.codec;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

public final class DateTimeTxt {

    private static final DateTimeFormatter frmt_decode_timestamptz;
    private static final DateTimeFormatter frmt_decode_timestamp;
    private static final DateTimeFormatter frmt_decode_date;
    private static final DateTimeFormatter frmt_decode_timetz;
    private static final DateTimeFormatter frmt_decode_time;

    private static final DateTimeFormatter frmt_encode_timestamptz;
    private static final DateTimeFormatter frmt_encode_timestamp;
    private static final DateTimeFormatter frmt_encode_date;
    private static final DateTimeFormatter frmt_encode_timetz;
    private static final DateTimeFormatter frmt_encode_time;

    static {
        frmt_decode_timestamptz = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .appendPattern("XXX")
                .toFormatter()
                .withZone(ZoneOffset.UTC);

        frmt_decode_timestamp = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .toFormatter();

        frmt_decode_date = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd")
                .toFormatter();

        frmt_decode_timetz = new DateTimeFormatterBuilder()
                .appendPattern("HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .appendPattern("XXX")
                .toFormatter()
                .withZone(ZoneOffset.UTC);

        frmt_decode_time = new DateTimeFormatterBuilder()
                .appendPattern("HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .toFormatter();

        frmt_encode_timestamptz = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSx")
                .withZone(ZoneOffset.UTC);

        frmt_encode_timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                .withZone(ZoneOffset.UTC);

        frmt_encode_date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC);

        frmt_encode_timetz = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSx");

        frmt_encode_time = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS");

    }

    //
    // Decoding
    //
    public static OffsetDateTime decodeTIMESTAMPTZ (final String input) {
        return OffsetDateTime.parse(input, frmt_decode_timestamptz);
    }

    public static LocalDateTime decodeTIMESTAMP (final String input) {
        return LocalDateTime.parse(input, frmt_decode_timestamp);
    }

    public static LocalDate decodeDATE (final String input) {
        return LocalDate.parse(input, frmt_decode_date);
    }

    public static OffsetTime decodeTIMETZ (final String input) {
        return OffsetTime.parse(input, frmt_decode_timetz);
    }

    public static LocalTime decodeTIME (final String input) {
        return LocalTime.parse(input, frmt_decode_time);
    }

    //
    // Encoding
    //

    // Temporal
    public static String encodeTIMESTAMPTZ (final Temporal t) {
        return frmt_encode_timestamptz.format(t);
    }

    public static String encodeTIMESTAMP (final Temporal t) {
        return frmt_encode_timestamp.format(t);
    }

    public static String encodeDATE (final Temporal t) {
        return frmt_encode_date.format(t);
    }

    public static String encodeTIMETZ (final Temporal t) {
        return frmt_encode_timetz.format(t);
    }

    public static String encodeTIME (final Temporal t) {
        return frmt_encode_time.format(t);
    }

}
