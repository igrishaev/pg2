package org.pg.msg.client;

import org.pg.Payload;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record SASLResponse(String clientFinalMessage) implements IClientMessage {

    public ByteBuffer encode(final Charset charset) {
        return new Payload()
                .addBytes(clientFinalMessage.getBytes(charset))
                .toByteBuffer('p');
    }
}
