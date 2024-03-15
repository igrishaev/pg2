package org.pg.msg.client;

import org.pg.enums.SourceType;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public record Close(SourceType sourceType, String source) implements IClientMessage {
    public ByteBuffer encode(final Charset charset) {
        final int size = 1 + 4 + 1 + source.length() + 1;
        final ByteBuffer bb = ByteBuffer.allocate(1 + 4 + 1 + source.length() + 1);
        bb.put((byte)'C');
        bb.putInt(size - 1);
        bb.put((byte)sourceType.getCode());
        bb.put(source.getBytes(StandardCharsets.UTF_8));
        bb.put((byte)0);
        return bb;
    }
}
