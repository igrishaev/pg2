package org.pg.msg;

public record SkippedMessage() {
    public static SkippedMessage INSTANCE = new SkippedMessage();
}
