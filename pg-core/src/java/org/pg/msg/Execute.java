package org.pg.msg;

import org.pg.Const;
import org.pg.error.PGError;
import org.pg.Payload;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Execute (String portal, long maxRows) implements IMessage {
    public Execute (final String portal, final long maxRows) {
        this.portal = portal;
        this.maxRows = maxRows;
        if (maxRows > Const.EXE_MAX_ROWS) {
            throw new PGError("Too many rows: %s", maxRows);
        }
    }
    public ByteBuffer encode(final Charset charset) {
        return new Payload()
            .addCString(portal, charset)
            .addUnsignedInteger(maxRows)
            .toByteBuffer('E');
    }
}
