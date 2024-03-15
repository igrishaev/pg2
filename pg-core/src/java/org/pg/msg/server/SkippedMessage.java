package org.pg.msg.server;

public record SkippedMessage() implements IServerMessage {
    public static SkippedMessage INSTANCE = new SkippedMessage();
}
