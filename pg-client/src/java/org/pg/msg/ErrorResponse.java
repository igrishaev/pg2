package org.pg.msg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

public record ErrorResponse (Map<String, String> fields) {

    public static ErrorResponse fromByteBuffer(final ByteBuffer buf, final Charset charset) {
        final Map<String, String> fields = FieldParser.parseFields(buf, charset);
        return new ErrorResponse(fields);
    }
}
