package org.pg.msg;

import org.pg.Payload;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record PasswordMessage (String password) implements IMessage {
    public ByteBuffer encode(final Charset charset) {
        return new Payload()
            .addCString(password, charset)
            .toByteBuffer('p');
    }
}
