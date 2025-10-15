package org.pg.msg.server;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record CommandComplete (String command) implements IServerMessage {
        public static CommandComplete fromByteBuffer(
                final ByteBuffer bb,
                final Charset charset
        ) {
                final String command = BBTool.getCString(bb, charset);
                return new CommandComplete(command);
        }
}
