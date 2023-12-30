package org.pg;

import org.pg.msg.ParameterDescription;
import org.pg.msg.Parse;

import java.util.Arrays;

public record PreparedStatement(
        Parse parse,
        ParameterDescription parameterDescription
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
