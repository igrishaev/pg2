package org.pg.msg.server;

public record BindComplete () implements IServerMessage {
    public static final BindComplete INSTANCE = new BindComplete();
}
