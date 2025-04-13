package org.pg.util;

import clojure.lang.Named;
import org.pg.Const;
import org.pg.enums.TxLevel;
import org.pg.error.PGError;

public final class SQLTool {

    public static String quoteChannel (final String sql) {
        return String.format("\"%s\"", sql.replaceAll("\"", "\"\""));
    }

    public static String SQLSetTxReadOnly = "SET TRANSACTION READ ONLY";

    public static String SQLSetTxLevel (final TxLevel level) {
        return "SET TRANSACTION ISOLATION LEVEL " + level.getCode();
    }

    public static String fullTypeName(final Object type) {
        final String[] parts = splitType(type);
        return parts[0] + '.' + parts[1];
    }

    public static String[] splitType(final Object type) {
        if (type instanceof String s) {
            final String[] parts = s.split("\\.", 2);
            if (parts.length == 1) {
                return new String[]{Const.defaultSchema, parts[0]};
            } else {
                return parts;
            }
        } else if (type instanceof Named nm) {
            String schema;
            schema = nm.getNamespace();
            if (schema == null) {
                schema = Const.defaultSchema;
            }
            return new String[]{schema, nm.getName()};
        } else {
            throw new PGError("unsupported postgres type, %s", TypeTool.repr(type));
        }
    }

    public static void main (final String[] args) {
        System.out.println(quoteChannel("aa\"a'aa"));
    }

}
