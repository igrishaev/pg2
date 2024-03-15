package org.pg.msg.server;

public record ParseComplete () implements IServerMessage {
    public static ParseComplete INSTANCE = new ParseComplete();
}
