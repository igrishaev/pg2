package org.pg.util;

import org.pg.error.PGError;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;

public class SocketTool {

    public static AsynchronousSocketChannel open (final SocketAddress address) {
        try {
            final AsynchronousSocketChannel ch = AsynchronousSocketChannel.open();
            Future result = ch.connect(address);
            result.get();
            return ch;
        } catch (IOException | InterruptedException | ExecutionException e) {
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
