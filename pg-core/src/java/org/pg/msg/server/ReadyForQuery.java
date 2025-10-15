package org.pg.msg.server;

import org.pg.enums.TXStatus;

public record ReadyForQuery (TXStatus txStatus) implements IServerMessage {
    public static ReadyForQuery fromBytes(final byte[] bytes) {
        final TXStatus status = TXStatus.ofChar((char) bytes[0]);
        return new ReadyForQuery(status);
    }
}
