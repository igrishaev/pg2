package org.pg.msg.server;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public record DataRow (
        short count,
        ByteBuffer buf
) implements IServerMessage {

    public int[] ToC() {
        buf.rewind();
        final short size = buf.getShort();
        final int[] ToC = new int[size * 2];
        for (short i = 0; i < size; i++) {
            final int length = buf.getInt();
            ToC[i * 2] = buf.position();
            ToC[i * 2 + 1] = length;
            if (length > 0) {
                BBTool.skip(buf, length);
            }
        }
        return ToC;
    }

    public static DataRow fromByteBuffer(final ByteBuffer buf) {
        return new DataRow(buf.getShort(), buf);
    }
}
