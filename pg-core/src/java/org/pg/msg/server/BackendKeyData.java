package org.pg.msg.server;

import java.nio.ByteBuffer;

public record BackendKeyData (int pid, int secretKey) implements IServerMessage {
    public static BackendKeyData fromByteBuffer(final ByteBuffer buf) {
        final int pid = buf.getInt();
        final int key = buf.getInt();
        return new BackendKeyData(pid, key);
    }
}
