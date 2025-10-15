package org.pg.msg.server;

import org.pg.util.ArrayTool;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record ParameterStatus (String param, String value) implements IServerMessage {
    public static ParameterStatus fromBytes(final byte[] bytes, final Charset charset) {
        final int[] off = {0};
        final String param = ArrayTool.readCString(bytes, off, charset);
        final String value = ArrayTool.readCString(bytes, off, charset);
        return new ParameterStatus(param, value);
    }
}
