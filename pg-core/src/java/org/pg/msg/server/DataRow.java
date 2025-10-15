package org.pg.msg.server;

import org.pg.util.ArrayTool;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record DataRow (
        short count,
        byte[] bytes
) implements IServerMessage {

    /*
    Return the Table of Content: a plain int array where [i]
    is the offset of data and [i + 1] is its length.
     */
    public int[] ToC() {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        BBTool.skip(bb, 2);
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

    @Override
    public String toString() {
        return String.format("DataRow[count=%s, buf=%s]",
                count,
                Arrays.toString(bytes)
        );
    }

    public static DataRow fromByteBuffer(final ByteBuffer bb) {
        // TODO: don't parse it?
        final short count = bb.getShort();
        return new DataRow(count, bb.array());
    }
}
