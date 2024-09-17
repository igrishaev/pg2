package org.pg.msg.server;

import org.pg.enums.Format;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record CopyOutResponse (
        Format format,
        short columnCount,
        Format[] columnFormats
) implements IServerMessage {

    @Override
    public String toString() {
        return String.format("CopyOutResponse[format=%s, columnCount=%s, columnFormats=%s]",
                format,
                columnCount,
                Arrays.toString(columnFormats)
        );
    }

    public static CopyOutResponse fromByteBuffer(final ByteBuffer buf) {
        final Format format = Format.ofShort(buf.get());
        final short columnCount = buf.getShort();
        final Format[] columnFormats = new Format[columnCount];
        for (short i = 0; i < columnCount; i++) {
            columnFormats[i] = Format.ofShort(buf.getShort());
        }
        return new CopyOutResponse(format, columnCount, columnFormats);
    }
}
