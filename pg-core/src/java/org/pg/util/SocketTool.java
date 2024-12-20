package org.pg.util;

import org.pg.error.PGError;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;

public class SocketTool {

    public static SocketChannel open (final SocketAddress address) {
        try {
            return SocketChannel.open(address);
        } catch (IOException e) {
            throw new PGError(e, "cannot open socket, address: %s", address);
        }
    }

    public static void close (final ByteChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            throw new PGError(e, "cannot close a byte channel: %s", channel);
        }
    }

}
