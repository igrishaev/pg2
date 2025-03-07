package org.pg.util;

import org.pg.error.PGErrorIO;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class SocketTool {

    public static SocketChannel open(final SocketAddress address) {
        try {
            return SocketChannel.open(address);
        } catch (final IOException e) {
            throw new PGErrorIO(e, "cannot open socket, address: %s, cause: %s", address, e.getMessage());
        }
    }

    public static SSLSocket open(final SSLContext sslContext,
                                 final Socket socket,
                                 final String host,
                                 final int port,
                                 final boolean autoClose) {
        try {
            return (SSLSocket) sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        } catch (final IOException e) {
            throw new PGErrorIO(e, "cannot open an SSL socket, socket: %s, host: %s, port: %s, cause: %s",
                    socket, host, port, e.getMessage());
        }
    }

}
