package org.pg.msg.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record SSLRequest (int sslCode) implements IClientMessage {
    public ByteBuffer encode(final Charset charset) {
        return ByteBuffer.allocate(8).putInt(8).putInt(sslCode);
    }
}
