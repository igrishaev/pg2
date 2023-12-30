package org.pg.msg;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public record DataRow (short valueCount, ByteBuffer[] values) {
    public static DataRow fromByteBuffer(final ByteBuffer buf) {

        final short size = buf.getShort();
        final ByteBuffer[] values = new ByteBuffer[size];
        for (short i = 0; i < size; i++) {
            final int len = buf.getInt();
            if (len == -1) {
                values[i] = null;
            }
            else {
                final ByteBuffer bufValue = buf.slice();
                bufValue.limit(len);
                BBTool.skip(buf, len);
                values[i] = bufValue;
            }
        }
        return new DataRow(size, values);
    }
}
