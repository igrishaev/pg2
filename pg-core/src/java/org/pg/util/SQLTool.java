package org.pg.util;

import org.pg.Const;
import org.pg.enums.TxLevel;

public final class SQLTool {

    public static String quoteChannel (final String sql) {
        return String.format("\"%s\"", sql.replaceAll("\"", "\"\""));
    }

    public static String SQLSetTxReadOnly = "SET TRANSACTION READ ONLY";

    public static String SQLSetTxLevel (final TxLevel level) {
        return "SET TRANSACTION ISOLATION LEVEL " + level.getCode();
    }

    public static String fullTypeName(final String type) {
        String schema;
        String name;
        final String[] parts = type.split("\\.", 2);
        if (parts.length == 1) {
            schema = Const.defaultSchema;
            name = parts[0];
        } else {
            schema = parts[0];
            name = parts[1];
        }
        return schema + '.' + name;
    }

    public static String[] splitType(final String type) {
        final String[] parts = type.split("\\.", 2);
        if (parts.length == 1) {
            return new String[]{Const.defaultSchema, parts[0]};
        } else {
            return parts;
        }
    }

    public static void main (final String[] args) {
        System.out.println(quoteChannel("aa\"a'aa"));
    }

}
