package org.pg.msg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.pg.enums.Format;
import org.pg.enums.OID;
import org.pg.Payload;

public record Bind (
        String portal,
        String statement,
        byte[][] values,
        OID[] OIDs,
        Format paramsFormat,
        Format columnFormat
) implements IMessage {

    public ByteBuffer encode(final Charset charset) {
        final Payload payload = new Payload()
                .addCString(portal)
                .addCString(statement)
                .addShort((short)1)
                .addShort(paramsFormat.toCode())
                .addUnsignedShort(values.length);

        for (byte[] bytes: values) {
            if (bytes == null) {
                payload.addInteger(-1);
            }
            else {
                payload.addInteger(bytes.length);
                payload.addBytes(bytes);
            }
        }

        payload.addShort((short)1);
        payload.addShort(columnFormat.toCode());

        return payload.toByteBuffer('B');
    }
}
