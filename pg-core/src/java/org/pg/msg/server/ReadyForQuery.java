package org.pg.msg.server;

import org.pg.enums.TXStatus;

import java.nio.ByteBuffer;

public record ReadyForQuery (TXStatus txStatus) implements IServerMessage {
    public static ReadyForQuery fromByteBuffer(final ByteBuffer bb) {
        final TXStatus status = TXStatus.ofChar((char) bb.get());
        return new ReadyForQuery(status);
    }
}
