package org.pg.util;

import org.pg.error.PGErrorIO;
import org.pg.Config;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class SocketTool {

    public static void setSocketOptions(Socket socket, Config config) {
        try {
            socket.setTcpNoDelay(config.SOTCPnoDelay());
            socket.setSoTimeout(config.SOTimeout());
            socket.setKeepAlive(config.SOKeepAlive());
            socket.setReceiveBufferSize(config.SOReceiveBufSize());
            socket.setSendBufferSize(config.SOSendBufSize());
        }
        catch (IOException e) {
            throw new PGErrorIO(e, "couldn't set socket options");
        }
    }

    public static SocketChannel open(final SocketAddress address) {
        try {
            return SocketChannel.open(address);
        } catch (IOException e) {
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
        } catch (IOException e) {
            throw new PGErrorIO(e, "cannot open an SSL socket, socket: %s, host: %s, port: %s, cause: %s",
                    socket, host, port, e.getMessage());
        }
    }


}
