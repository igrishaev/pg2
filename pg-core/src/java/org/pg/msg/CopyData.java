package org.pg.msg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record CopyData (ByteBuffer buf) implements IMessage {

    public ByteBuffer encode(final Charset charset) {
        buf.rewind();
        final int size = buf.limit();
        final ByteBuffer result = ByteBuffer.allocate(1 + 4 + size);
        result.put((byte)'d');
        result.putInt(4 + size);
        result.put(buf);
        return result;
    }

    public static CopyData fromByteBuffer(final ByteBuffer buf) {
        return new CopyData(buf);
    }
}
