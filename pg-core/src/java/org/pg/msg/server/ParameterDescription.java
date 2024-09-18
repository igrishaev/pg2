package org.pg.msg.server;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record ParameterDescription (
        int paramCount,
        int[] OIDs
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
        final int[] OIDs = new int[count];
        for (int i = 0; i < count; i++) {
            OIDs[i] = buf.getInt();
        }
        return new ParameterDescription(count, OIDs);
    }
}
