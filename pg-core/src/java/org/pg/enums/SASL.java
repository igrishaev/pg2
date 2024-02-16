package org.pg.enums;

import org.pg.error.PGError;

public enum SASL {

    SCRAM_SHA_256("SCRAM-SHA-256"),
    SCRAM_SHA_256_PLUS("SCRAM-SHA-256-PLUS");

    private final String code;

    SASL(final String code) {
        this.code = code;
    }

    public String toCode () {
        return this.code;
    }

    public static SASL ofCode (final String input) {
        return switch (input) {
            case "SCRAM-SHA-256" -> SCRAM_SHA_256;
            case "SCRAM-SHA-256-PLUS" -> SCRAM_SHA_256_PLUS;
            default -> throw new PGError("cannot parse SASL type: %s", input);
        };
    }
}
