package org.pg.util;

import org.pg.error.PGError;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class SocketTool {

    public static SocketChannel open (final SocketAddress address) {
        try {
            return SocketChannel.open(address);
        } catch (IOException e) {
            throw new PGError(e, "cannot open socket, address: %s", address);
        }
    }

    public static Socket socket (final String host, final int port) {
        try {
            return new Socket(host, port);
        }
        catch (IOException e) {
            throw new PGError(
                    e,
                    "cannot open a socket, host: %s, port: %s",
                    host,
                    port
            );
        }
    }
}
