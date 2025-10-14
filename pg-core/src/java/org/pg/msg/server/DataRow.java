package org.pg.msg.server;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record DataRow (
        short count,
        ByteBuffer buf
) implements IServerMessage {

    /*
    Return the Table of Content: a plain int array where [i]
    is the offset of data and [i + 1] is its length.
     */
    public int[] ToC() {
        // clone buffer to make the method thread-safe
        final ByteBuffer bb = buf.duplicate();
        bb.rewind();
        final short size = bb.getShort();
        final int[] ToC = new int[size * 2];
        for (short i = 0; i < size; i++) {
            final int length = bb.getInt();
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
                Arrays.toString(buf.array())
        );
    }

    public static DataRow fromByteBuffer(final ByteBuffer buf) {
        return new DataRow(buf.getShort(), buf);
    }
}
