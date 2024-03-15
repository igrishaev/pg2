package org.pg.msg.server;

public record NoData() implements IServerMessage {
    public static final NoData INSTANCE = new NoData();
}
