package org.pg.msg.server;

import org.pg.util.PGIO;

import java.nio.ByteBuffer;

public record AuthenticationMD5Password (byte[] salt) implements IServerMessage {

    public static final int status = 5;

    public static AuthenticationMD5Password fromPGIO(final PGIO pgio) {
        final byte[] salt = new byte[4];
        pgio.readFully(salt);
        return new AuthenticationMD5Password(salt);
    }

    public static AuthenticationMD5Password fromByteBuffer(ByteBuffer buf) {
        final byte[] salt = new byte[4];
        buf.get(salt);
        return new AuthenticationMD5Password(salt);
    }
}
