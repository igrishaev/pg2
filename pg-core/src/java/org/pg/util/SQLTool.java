package org.pg.util;

import org.pg.enums.TxLevel;

public final class SQLTool {

    public static String quoteChannel (final String sql) {
        return String.format("\"%s\"", sql.replaceAll("\"", "\"\""));
    }

    public static String SQLSetTxReadOnly = "SET TRANSACTION READ ONLY";

    public static String SQLSetTxLevel (final TxLevel level) {
        return "SET TRANSACTION ISOLATION LEVEL " + level.getCode();
    }

    public static void main (final String[] args) {
        System.out.println(quoteChannel("aa\"a'aa"));
    }

}
