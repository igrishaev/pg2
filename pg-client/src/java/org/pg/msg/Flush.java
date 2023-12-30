package org.pg.msg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Flush () implements IMessage {
    public final static Flush INSTANCE = new Flush();
    private final static ByteBuffer buf = ByteBuffer.wrap(new byte[] {
            (byte)'H',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 4
    });
    public ByteBuffer encode(Charset encoding) {
        return buf;
    }
}
