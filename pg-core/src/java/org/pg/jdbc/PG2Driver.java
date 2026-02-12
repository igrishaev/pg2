package org.pg.jdbc;

import org.pg.Config;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class PG2Driver implements java.sql.Driver {

    static {
        try {
            DriverManager.registerDriver(new PG2Driver());
        } catch (SQLException e) {
            throw new RuntimeException("Cannot register PG2 JDBC driver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        // TODO: parse config from URL
        final Config config = Config.standard("foo", "bar");
        return null;
        // return org.pg.Connection.connect(config);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return false;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        // TODO
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        // TODO
        return 0;
    }

    @Override
    public int getMinorVersion() {
        // TODO
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() {
        return null;
    }
}
