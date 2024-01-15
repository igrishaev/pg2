package org.pg.enums;

public enum SourceType {
    STATEMENT('S'), PORTAL('P');

    private final char code;

    SourceType(final char code) {
        this.code = code;
    }

    public char getCode() {
        return this.code;
    }
}
