package org.pg.msg.server;

import org.pg.util.ArrayTool;

public record BackendKeyData (int pid, int secretKey) implements IServerMessage {
    public static BackendKeyData fromBytes(final byte[] bytes) {
        final int[] off = {0};
        final int pid = ArrayTool.readInt(bytes, off);
        final int key = ArrayTool.readInt(bytes, off);
        return new BackendKeyData(pid, key);
    }
}
