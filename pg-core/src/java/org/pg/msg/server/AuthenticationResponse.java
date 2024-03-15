package org.pg.msg.server;

import org.pg.error.PGError;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record AuthenticationResponse (int status) implements IServerMessage {

    public static IServerMessage fromByteBuffer(final ByteBuffer buf, Charset charset) {
        final int status = buf.getInt();
        return switch (status) {
            case  0 -> AuthenticationOk.INSTANCE;
            case  3 -> AuthenticationCleartextPassword.INSTANCE;
            case  5 -> AuthenticationMD5Password.fromByteBuffer(buf);
            case 10 -> AuthenticationSASL.fromByteBuffer(buf);
            case 11 -> AuthenticationSASLContinue.fromByteBuffer(buf, charset);
            case 12 -> AuthenticationSASLFinal.fromByteBuffer(buf, charset);
            default -> throw new PGError(
                    "Unknown auth response message, status: %s",
                    status
            );
        };

    }
}
