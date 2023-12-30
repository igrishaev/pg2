package org.pg.enums;

import org.pg.PGError;

public enum TXStatus {
    IDLE, TRANSACTION, ERROR;

    public static TXStatus ofChar (char c) {
        return switch (c) {
            case 'I' -> IDLE;
            case 'T' -> TRANSACTION;
            case 'E' -> ERROR;
            default -> throw new PGError("wrong tx status: %s", c);
        };
    }
}
