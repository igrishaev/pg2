package org.pg.msg;

import org.pg.Payload;
import org.pg.enums.SourceType;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public record Describe(SourceType sourceType, String source) implements IMessage {
    public ByteBuffer encode(final Charset charset) {
        final int size = 1 + 4 + 1 + source.length() + 1;
        final ByteBuffer bb = ByteBuffer.allocate(size);
        bb.put((byte)'D');
        bb.putInt(size - 1);
        bb.put((byte)sourceType.getCode());
        bb.put(source.getBytes(StandardCharsets.UTF_8));
        bb.put((byte)0);
        return bb;
//        return new Payload()
//                .addByte((byte)sourceType.getCode())
//                .addCString(source, charset)
//                .toByteBuffer('D');
    }
}
