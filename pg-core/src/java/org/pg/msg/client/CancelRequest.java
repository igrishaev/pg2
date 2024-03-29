package org.pg.msg.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record CancelRequest(
        int code,
        int pid,
        int secretKey
) implements IClientMessage {

    public ByteBuffer encode(final Charset charset) {
        final ByteBuffer buf = ByteBuffer.allocate(32);
        buf.putInt(16);
        buf.putInt(code);
        buf.putInt(pid);
        buf.putInt(secretKey);
        return buf;
    }
}
