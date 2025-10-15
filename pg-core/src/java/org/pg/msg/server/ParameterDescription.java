package org.pg.msg.server;

import org.pg.util.ArrayTool;

import java.util.Arrays;

public record ParameterDescription (
        int paramCount,
        int[] oids
) implements IServerMessage {

    @Override
    public String toString() {
        return String.format("ParameterDescription[paramCount=%s, oids=%s]",
                paramCount,
                Arrays.toString(oids)
        );
    }

    public static ParameterDescription fromBytes(final byte[] bytes) {
        final int[] off = {0};
        final int count = Short.toUnsignedInt(ArrayTool.readShort(bytes, off));
        final int[] OIDs = new int[count];
        for (int i = 0; i < count; i++) {
            OIDs[i] = ArrayTool.readInt(bytes, off);
        }
        return new ParameterDescription(count, OIDs);
    }
}
