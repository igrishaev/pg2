package org.pg.enums;

public enum TxLevel {

    @SuppressWarnings("unused")
    READ_UNCOMMITTED("READ UNCOMMITTED"),

    @SuppressWarnings("unused")
    READ_COMMITTED("READ COMMITTED"),

    @SuppressWarnings("unused")
    REPEATABLE_READ("REPEATABLE READ"),

    @SuppressWarnings("unused")
    SERIALIZABLE("SERIALIZABLE"),

    @SuppressWarnings("unused")
    NONE("NONE");

    private final String code;

    TxLevel(final String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    @SuppressWarnings("unused")
    public static TxLevel ofCode (final String code) {
        return TxLevel.valueOf(code);
    }
}
