package org.pg.msg;

import org.pg.msg.client.IClientMessage;
import org.pg.msg.server.IServerMessage;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record CopyDone () implements IClientMessage, IServerMessage {
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
