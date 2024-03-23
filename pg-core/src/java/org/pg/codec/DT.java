package org.pg.codec;

import java.time.*;
import java.util.Date;

public class DT {

    public static LocalDateTime toLocalDateTime (final ZonedDateTime zonedDateTime) {
        return LocalDateTime.ofInstant(zonedDateTime.toInstant(), ZoneOffset.UTC);
    }

    public static LocalTime toLocalTime(final OffsetTime offsetTime) {
        return offsetTime.toLocalTime();
    }

    public static OffsetTime toOffsetTime(final LocalTime localTime) {
        return localTime.atOffset(ZoneOffset.UTC);
    }

    public static LocalDate toLocalDate(final OffsetDateTime offsetDateTime) {
        return offsetDateTime.toLocalDate();
    }

    public static LocalDate toLocalDate(final LocalDateTime localDateTime) {
        return localDateTime.toLocalDate();
    }

    public static LocalDate toLocalDate(final Date date) {
        return LocalDate.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public static LocalDate toLocalDate(final Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }

    public static LocalDate toLocalDate(final ZonedDateTime zonedDateTime) {
        return zonedDateTime.toLocalDate();
    }

    public static Instant toInstant(final LocalDate localDate) {
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant toInstant(final ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant();
    }

    public static Instant toInstant(final Date date) {
        return date.toInstant();
    }

    public static Instant toInstant(final LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC);
    }
}
