package org.pg.msg.server;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record ParameterStatus (String param, String value) implements IServerMessage {
    public static ParameterStatus fromByteBuffer(final ByteBuffer bb, final Charset charset) {
        final String param = BBTool.getCString(bb, charset);
        final String value = BBTool.getCString(bb, charset);
        return new ParameterStatus(param, value);
    }
}
