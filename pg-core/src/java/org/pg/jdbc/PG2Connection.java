package org.pg.jdbc;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import static org.pg.jdbc.SQLError.*;

public class PG2Connection implements Connection {

    private final org.pg.Connection conn;
    private boolean autoCommit;
    private boolean readOnly;
    private int isolationLevel;

    public PG2Connection(org.pg.Connection conn) {
        this.conn = conn;
    }

    @Override
    public Statement createStatement() {
        return new PG2Statement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) {
        final org.pg.PreparedStatement statement = conn.prepare(sql);
        return new PG2PreparedStatement(conn, statement);
    }

    @Override
    public CallableStatement prepareCall(String sql) {
        return new PG2CallableStatement(sql);
    }

    @Override
    public String nativeSQL(String sql) {
        return sql;
    }

    private final String sqlReadOnly = "SET SESSION CHARACTERISTICS AS TRANSACTION READ ONLY";
    private final String sqlReadWrite = "SET SESSION CHARACTERISTICS AS TRANSACTION READ WRITE";

    @Override
    public void setAutoCommit(boolean autoCommit) {
        if (this.autoCommit == autoCommit) {
            return;
        }

        if (!this.autoCommit) {
            commit();
        }

        // TODO: read only?

        this.autoCommit = autoCommit;
    }

    @Override
    public boolean getAutoCommit() {
        return autoCommit;
    }

    @Override
    public void commit() {
        conn.commit();
    }

    @Override
    public void rollback() {
        conn.rollback();
    }

    @Override
    public void close() {
        conn.close();
    }

    @Override
    public boolean isClosed() {
        return conn.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() {
        return new PG2DatabaseMetaData(conn);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        if (conn.isTransaction()) {
            throw sqlError("cannot set read only in the middle of a transaction");
        }

        // TODO

        this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        throw notSupported("setCatalog is not supported");
    }

    @Override
    public String getCatalog() throws SQLException {
        throw notSupported("getCatalog is not supported");
    }

    private static String getIsolationLevelSQL(final int level) throws SQLException {
        return switch (level) {
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> throw sqlError("unknown isolation level: %s", level);
        };
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (conn.isTransaction()) {
            throw sqlError("cannot change isolation level in the middle of a transaction");
        }
        this.isolationLevel = level;
        final String levelSql = getIsolationLevelSQL(level);
        conn.query("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL " + levelSql);
    }

    @Override
    public int getTransactionIsolation() {
        return isolationLevel;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        // TODO
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        // TODO
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return Map.of();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

    }

    @Override
    public void setHoldability(int holdability) throws SQLException {

    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return null;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return null;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {

    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return null;
    }

    @Override
    public Clob createClob() throws SQLException {
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return null;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return false;
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {

    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {

    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return "";
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return null;
    }

    @Override
    public void setSchema(String schema) throws SQLException {

    }

    @Override
    public String getSchema() throws SQLException {
        return "";
    }

    @Override
    public void abort(Executor executor) throws SQLException {

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
