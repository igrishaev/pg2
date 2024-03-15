package org.pg.msg.server;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public record DataRow (
        int[][] ToC,
        byte[] payload
) implements IServerMessage {

    public int count () {
        return ToC.length;
    }

    public static DataRow fromByteBuffer(final ByteBuffer buf) {

        final short size = buf.getShort();
        final int[][] ToC = new int[size][2];

        for (short i = 0; i < size; i++) {
            final int length = buf.getInt();
            ToC[i][0] = buf.position();
            ToC[i][1] = length;
            if (length > 0) {
                BBTool.skip(buf, length);
            }
        }

        return new DataRow(ToC, buf.array());
    }
}
