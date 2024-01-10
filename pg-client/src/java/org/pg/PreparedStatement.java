package org.pg;

import org.pg.msg.ParameterDescription;
import org.pg.msg.Parse;
import org.pg.msg.RowDescription;

import java.util.Arrays;

public record PreparedStatement (
        Parse parse,
        ParameterDescription parameterDescription,
        RowDescription rowDescription
) {

    @Override
    public String toString() {
        return String.format(
                "<Prepared statement, name: %s, param(s): %s, OIDs: %s, SQL: %s>",
                parse.statement(),
                parameterDescription.paramCount(),
                Arrays.toString(parameterDescription.OIDs()),
                parse.query()
        );
    }
}
