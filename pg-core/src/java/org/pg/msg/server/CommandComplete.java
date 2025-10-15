package org.pg.msg.server;

import org.pg.util.ArrayTool;

import java.nio.charset.Charset;

public record CommandComplete (String command) implements IServerMessage {
        public static CommandComplete fromBytes(
                final byte[] bytes,
                final Charset charset
        ) {
                final int[] off = {0};
                final String command = ArrayTool.readCString(bytes, off, charset);
                return new CommandComplete(command);
        }
}
