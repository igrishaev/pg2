package org.pg.msg;

import org.pg.Payload;
import org.pg.enums.SourceType;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Describe(SourceType sourceType, String source) implements IMessage {
    public ByteBuffer encode(final Charset charset) {
        return new Payload()
                .addByte((byte)sourceType.getCode())
                .addCString(source, charset)
                .toByteBuffer('D');
    }
}
