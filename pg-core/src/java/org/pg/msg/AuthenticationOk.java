package org.pg.msg;

public record AuthenticationOk() {
    public static final int status = 0;
    public static final AuthenticationOk INSTANCE = new AuthenticationOk();
}
