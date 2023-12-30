package org.pg.msg;

import java.nio.ByteBuffer;

public record BackendKeyData (int pid, int secretKey) {
    public static BackendKeyData fromByteBuffer(final ByteBuffer buf) {
        final int pid = buf.getInt();
        final int key = buf.getInt();
        return new BackendKeyData(pid, key);
    }
}
