package org.pg.msg.client;

import org.pg.Payload;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record CopyFail (String errorMessage) implements IClientMessage {
    public ByteBuffer encode(final Charset charset) {
        return new Payload()
            .addCString(errorMessage, charset)
            .toByteBuffer('f');
    }
}
