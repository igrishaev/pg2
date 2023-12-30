package org.pg.codec;

import java.nio.ByteBuffer;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

public class DateTimeBin {

    private static final Duration PG_DIFF;

    static {
        PG_DIFF = Duration.between(
                Instant.EPOCH,
                LocalDate.of(2000, 1, 1).atStartOfDay(ZoneOffset.UTC)
        );
    }

    //
    // Decode
    //
    public static OffsetDateTime decodeTIMESTAMPTZ (final ByteBuffer buf) {
        final long secsAndMicros = buf.getLong();
        final long secs = secsAndMicros / 1_000_000 + PG_DIFF.toSeconds();
        final long nanoSec = secsAndMicros % 1_000_000 * 1_000;
        final Instant inst = Instant.ofEpochSecond(secs, nanoSec);
        return OffsetDateTime.ofInstant(inst, ZoneOffset.UTC);
    }

    public static LocalDateTime decodeTIMESTAMP (final ByteBuffer buf) {
        final long secsAndMicros = buf.getLong();
        final long secs = secsAndMicros / 1_000_000 + PG_DIFF.toSeconds();
        final long nanoSec = secsAndMicros % 1_000_000 * 1_000;
        return LocalDateTime.ofEpochSecond(secs, (int)nanoSec, ZoneOffset.UTC);
    }

    public static LocalDate decodeDATE (final ByteBuffer buf) {
        final int days = buf.getInt();
        return LocalDate.ofEpochDay(days + PG_DIFF.toDays());
    }

    public static OffsetTime decodeTIMETZ (final ByteBuffer buf) {
        final long micros = buf.getLong();
        final int offset = buf.getInt();
        return OffsetTime.of(
                LocalTime.ofNanoOfDay(micros * 1_000),
                ZoneOffset.ofTotalSeconds(-offset)
        );
    }

    public static LocalTime decodeTIME (final ByteBuffer buf) {
        final long micros = buf.getLong();
        return LocalTime.ofNanoOfDay(micros * 1_000);
    }

    //
    // Encode
    //
    public static ByteBuffer encodeTIME (final Temporal t) {
        final long micros = t.getLong(ChronoField.MICRO_OF_DAY);
        final ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(micros);
        return buf;
    }

    public static ByteBuffer encodeTIMETZ (final Temporal t) {
        final long micros = t.getLong(ChronoField.MICRO_OF_DAY);
        final long offset = -t.getLong(ChronoField.OFFSET_SECONDS);
        final ByteBuffer buf = ByteBuffer.allocate(12);
        buf.putLong(micros);
        buf.putInt((int)offset);
        return buf;
    }

    public static ByteBuffer encodeDATE (final Temporal t) {
        final long days = t.getLong(ChronoField.EPOCH_DAY) - PG_DIFF.toDays();
        final ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt((int)days);
        return buf;
    }

    public static ByteBuffer encodeTIMESTAMP (final Temporal t) {
        final long secs = t.getLong(ChronoField.INSTANT_SECONDS) - PG_DIFF.toSeconds();
        final long micros = t.getLong(ChronoField.MICRO_OF_SECOND);
        final long sum = secs * 1_000_000 + micros;
        final ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(sum);
        return buf;
    }

    public static ByteBuffer encodeTIMESTAMPTZ (final Temporal t) {
        final long secs = t.getLong(ChronoField.INSTANT_SECONDS) - PG_DIFF.toSeconds();
        final long micros = t.getLong(ChronoField.MICRO_OF_SECOND);
        final long sum = secs * 1_000_000 + micros;
        final ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(sum);
        return buf;
    }

}
