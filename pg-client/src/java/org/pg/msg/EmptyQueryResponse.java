package org.pg.msg;

public record EmptyQueryResponse() {
    public final static EmptyQueryResponse INSTANCE = new EmptyQueryResponse();
}
