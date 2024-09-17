package org.pg.msg.server;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record AuthenticationMD5Password (byte[] salt) implements IServerMessage {

    public static final int status = 5;

    @Override
    public String toString() {
        return String.format("AuthenticationMD5Password[status=%s, salt=%s]",
                status,
                Arrays.toString(salt)
        );
    }

    public static AuthenticationMD5Password fromByteBuffer(ByteBuffer buf) {
        final byte[] salt = new byte[4];
        buf.get(salt);
        return new AuthenticationMD5Password(salt);
    }
}
