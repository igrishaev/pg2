package org.pg.jdbc;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

public class SQLError {

    public static SQLException sqlError(final String template, final Object... args) {
        return new SQLException(String.format(template, args));
    }

    public static SQLFeatureNotSupportedException notSupported(final String message) {
        return new SQLFeatureNotSupportedException(message);
    }

    @SuppressWarnings("unused")
    public static SQLFeatureNotSupportedException notSupported(final String template, final Object... args) {
        return new SQLFeatureNotSupportedException(String.format(template, args));
    }


}
