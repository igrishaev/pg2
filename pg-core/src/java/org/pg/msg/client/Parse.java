package org.pg.msg.client;

import org.pg.enums.OID;
import org.pg.error.PGError;
import org.pg.Payload;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public record Parse (String statement,
                     String query,
                     OID[] OIDs)
        implements IClientMessage {

    @Override
    public String toString() {
        return String.format("Parse[statement=%s, query=%s, OIDs=%s]",
                statement,
                query,
                Arrays.toString(OIDs)
        );
    }

    public ByteBuffer encode(final Charset charset) {

        final int OIDCount = OIDs.length;

        if (OIDCount > 0xFFFF) {
            throw new PGError(
                    "Too many OIDs! OID count: %s, query: %s",
                    OIDCount, query
            );
        }

        final Payload payload = new Payload();

        payload
            .addCString(statement, charset)
            .addCString(query, charset)
            .addUnsignedShort(OIDCount);

        for (OID oid: OIDs) {
            payload.addInteger(oid.toInt());
        }

        return payload.toByteBuffer('P');
    }

}
