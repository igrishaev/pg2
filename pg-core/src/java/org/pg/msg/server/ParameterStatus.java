package org.pg.msg.server;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record ParameterStatus (String param, String value) implements IServerMessage {
    public static ParameterStatus fromByteBuffer(final ByteBuffer buf, final Charset charset) {
        final String param = BBTool.getCString(buf, charset);
        final String value = BBTool.getCString(buf, charset);
        return new ParameterStatus(param, value);
    }
}
