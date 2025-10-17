package org.pg.msg.server;

import org.pg.util.ArrayTool;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record DataRow (
        byte[] bytes
) implements IServerMessage {

    /*
    Return the Table of Content: a plain int array where [i]
    is the offset of data and [i + 1] is its length.
     */
    public int[] ToC() {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final short count = bb.getShort();
        int length;
        final int[] ToC = new int[count * 2];
        for (short i = 0; i < count; i++) {
            length = bb.getInt();
            ToC[i * 2] = bb.position();
            ToC[i * 2 + 1] = length;
            if (length > 0) {
                BBTool.skip(bb, length);
            }
        }
        return ToC;
    }

    public int count() {
        return ArrayTool.readShort(bytes, 0);
    }

    @Override
    public String toString() {
        return String.format("DataRow[count=%s, buf=%s]",
                count(),
                Arrays.toString(bytes)
        );
    }

    public static DataRow fromBytes(final byte[] buf) {
        return new DataRow(buf);
    }
}
