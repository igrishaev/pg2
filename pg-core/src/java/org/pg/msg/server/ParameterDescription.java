package org.pg.msg.server;

import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record ParameterDescription (
        int paramCount,
        OID[] OIDs
) implements IServerMessage {

    @Override
    public String toString() {
        return String.format("ParameterDescription[paramCount=%s, OIDs=%s]",
                paramCount,
                Arrays.toString(OIDs)
        );
    }

    public static ParameterDescription fromByteBuffer(final ByteBuffer buf) {
        final int count = Short.toUnsignedInt(buf.getShort());
        final OID[] OIDs = new OID[count];
        for (int i = 0; i < count; i++) {
            OIDs[i] = OID.ofInt(buf.getInt());
        }
        return new ParameterDescription(count, OIDs);
    }
}
