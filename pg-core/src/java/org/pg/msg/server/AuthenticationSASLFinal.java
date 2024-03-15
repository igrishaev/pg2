package org.pg.msg.server;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record AuthenticationSASLFinal (String serverFinalMessage) implements IServerMessage {

    public static final int status = 12;

    public static AuthenticationSASLFinal fromByteBuffer(
            final ByteBuffer buf,
            final Charset charset
    ) {
        final String message = BBTool.getRestString(buf, charset);
        return new AuthenticationSASLFinal(message);
    }
}
