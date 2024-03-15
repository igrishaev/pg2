package org.pg.msg.client;

import org.pg.Payload;
import org.pg.enums.SASL;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record SASLInitialResponse(
        SASL saslType,
        String clientFirstMessage
) implements IClientMessage {

    public ByteBuffer encode(final Charset charset) {
        final Payload payload = new Payload().addCString(saslType().toCode());
        if (clientFirstMessage.isEmpty()) {
            payload.addInteger(-1);
        }
        else {
            final byte[] bytes = clientFirstMessage.getBytes(charset);
            payload.addInteger(bytes.length);
            payload.addBytes(bytes);
        }
        return payload.toByteBuffer('p');
    }

}
