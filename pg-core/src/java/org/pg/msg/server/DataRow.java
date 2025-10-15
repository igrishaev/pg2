package org.pg.msg.server;

import org.pg.util.ArrayTool;

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
        final int[] off = {2};
        int length;
        final int[] ToC = new int[count * 2];
        for (short i = 0; i < count; i++) {
            length = ArrayTool.readInt(bytes, off);
            ToC[i * 2] = off[0];
            ToC[i * 2 + 1] = length;
            if (length > 0) {
                ArrayTool.skip(length, off);
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

    public static DataRow fromBytes(final byte[] bytes) {
        final short count = ArrayTool.readShort(bytes, 0);
        return new DataRow(count, bytes);
    }
}
