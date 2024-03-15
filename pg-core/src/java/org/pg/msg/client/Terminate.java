package org.pg.msg.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Terminate () implements IClientMessage {
    public final static Terminate INSTANCE = new Terminate();
    private final static ByteBuffer buf = ByteBuffer.wrap(new byte[] {
            (byte)'X',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 4
    });
    public ByteBuffer encode(Charset encoding) {
        return buf;
    }
}
