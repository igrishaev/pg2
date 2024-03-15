package org.pg.msg.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Sync () implements IClientMessage {
    public final static byte[] BYTES = new byte[] {
            (byte)'S',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 4
    };
    public final static Sync INSTANCE = new Sync();
    private final static ByteBuffer buf = ByteBuffer.wrap(BYTES);
    public ByteBuffer encode(Charset encoding) {
        return buf;
    }
}
