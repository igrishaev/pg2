package org.pg.msg.server;

import org.pg.enums.Format;

import java.nio.ByteBuffer;

public record CopyInResponse(
        Format format,
        short columnCount,
        Format[] columnFormats
) implements IServerMessage {

    public static CopyInResponse fromByteBuffer (final ByteBuffer buf) {
        final Format format = Format.ofShort(buf.get());
        final short columnCount = buf.getShort();
        final Format[] columnFormats = new Format[columnCount];
        for (short i = 0; i < columnCount; i++) {
            columnFormats[i] = Format.ofShort(buf.getShort());
        }
        return new CopyInResponse(format, columnCount, columnFormats);
    }

}
