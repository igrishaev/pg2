package org.pg.msg;

import org.pg.enums.OID;

import java.nio.ByteBuffer;

public record ParameterDescription (
        int paramCount,
        OID[] OIDs
) {

    public static ParameterDescription fromByteBuffer(final ByteBuffer buf) {
        final int count = Short.toUnsignedInt(buf.getShort());
        final OID[] OIDs = new OID[count];
        for (int i = 0; i < count; i++) {
            OIDs[i] = OID.ofInt(buf.getInt());
        }
        return new ParameterDescription(count, OIDs);
    }
}
