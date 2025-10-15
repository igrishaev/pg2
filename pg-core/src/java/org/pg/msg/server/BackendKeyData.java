package org.pg.msg.server;

import java.nio.ByteBuffer;

public record BackendKeyData (int pid, int secretKey) implements IServerMessage {
    public static BackendKeyData fromByteBuffer(final ByteBuffer bb) {
        final int pid = bb.getInt();
        final int key = bb.getInt();
        return new BackendKeyData(pid, key);
    }
}
