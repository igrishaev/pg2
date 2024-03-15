package org.pg.msg.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Flush () implements IClientMessage {
    public static final byte[] BYTES = new byte[] {
            (byte)'H',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 4
    };
    public final static Flush INSTANCE = new Flush();
    private final static ByteBuffer buf = ByteBuffer.wrap(BYTES);
    public ByteBuffer encode(Charset encoding) {
        return buf;
    }
}
