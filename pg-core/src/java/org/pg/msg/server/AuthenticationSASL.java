package org.pg.msg.server;

import org.pg.enums.SASL;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public record AuthenticationSASL (HashSet<SASL> SASLTypes) implements IServerMessage {

    public static final int status = 10;


    public static AuthenticationSASL fromByteBuffer(final ByteBuffer buf) {
        final HashSet<SASL> types = new HashSet<>();
        while (true) {
            final String type = BBTool.getCString(buf, StandardCharsets.UTF_8);
            if (type.isEmpty()) {
                break;
            }
            types.add(SASL.ofCode(type));
        }
        return new AuthenticationSASL(types);
    }

    public boolean isScramSha256 () {
        return SASLTypes.contains(SASL.SCRAM_SHA_256);
    }

    public boolean isScramSha256Plus () {
        return SASLTypes.contains(SASL.SCRAM_SHA_256_PLUS);
    }

}
