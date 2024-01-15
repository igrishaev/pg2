package org.pg.msg;

public record CloseComplete () {
    public static final CloseComplete INSTANCE = new CloseComplete();
}
