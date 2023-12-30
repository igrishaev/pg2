package org.pg.msg;

import org.pg.enums.TXStatus;

import java.nio.ByteBuffer;

public record ReadyForQuery (TXStatus txStatus) {
    public static ReadyForQuery fromByteBuffer(final ByteBuffer buf) {
        final TXStatus status = TXStatus.ofChar((char) buf.get());
        return new ReadyForQuery(status);
    }
}
