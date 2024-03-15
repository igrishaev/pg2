package org.pg.msg.server;

public record EmptyQueryResponse() implements IServerMessage {
    public final static EmptyQueryResponse INSTANCE = new EmptyQueryResponse();
}
