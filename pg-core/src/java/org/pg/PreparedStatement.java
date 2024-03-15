package org.pg;

import org.pg.msg.server.ParameterDescription;
import org.pg.msg.client.Parse;
import org.pg.msg.server.RowDescription;

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
