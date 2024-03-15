package org.pg.msg.server;

public record CloseComplete () implements IServerMessage {
    public static final CloseComplete INSTANCE = new CloseComplete();
}
