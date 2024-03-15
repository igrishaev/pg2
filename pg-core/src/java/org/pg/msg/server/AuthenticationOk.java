package org.pg.msg.server;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record AuthenticationOk() implements IServerMessage {

    public static final int status = 0;
    public static final AuthenticationOk INSTANCE = new AuthenticationOk();

    public AuthenticationOk fromByteBuffer(ByteBuffer buf, Charset charset) {
        return INSTANCE;
    }
}
