package org.pg.msg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record CopyDone () implements IMessage {
    public final static CopyDone INSTANCE = new CopyDone();
    private final static ByteBuffer buf = ByteBuffer.wrap(new byte[] {
            (byte)'c',
            (byte) 0,
            (byte) 0,
            (byte) 0,
            (byte) 4
    });
    public ByteBuffer encode(Charset encoding) {
        return buf;
    }
}
