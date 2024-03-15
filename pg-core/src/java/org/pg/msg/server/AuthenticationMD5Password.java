package org.pg.msg.server;

import java.nio.ByteBuffer;

public record AuthenticationMD5Password (byte[] salt) implements IServerMessage {

    public static final int status = 5;

    public static AuthenticationMD5Password fromByteBuffer(ByteBuffer buf) {
        final byte[] salt = new byte[4];
        buf.get(salt);
        return new AuthenticationMD5Password(salt);
    }
}
