package org.pg.msg;

import java.nio.ByteBuffer;

public record AuthenticationMD5Password (byte[] salt) {
    public static final int status = 5;
    public static AuthenticationMD5Password fromByteBuffer(final ByteBuffer buf) {
        final byte[] salt = new byte[4];
        buf.get(salt);
        return new AuthenticationMD5Password(salt);
    }
}
