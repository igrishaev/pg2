package org.pg;

import org.pg.error.PGErrorIO;
import org.pg.util.IOTool;
import org.pg.util.SSLTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

public class PGSocketChannel implements PGIOChannel {
    private final InetSocketAddress address;
    private final Socket socket;

    protected PGSocketChannel(final InetSocketAddress address, final Socket socket) {
        this.address = address;
        this.socket = socket;
    }

    private static void setSocketOptions(final Socket socket, final Config config) {
        try {
            socket.setTcpNoDelay(config.SOTCPnoDelay());
            socket.setSoTimeout(config.SOTimeout());
            socket.setKeepAlive(config.SOKeepAlive());
            socket.setReceiveBufferSize(config.SOReceiveBufSize());
            socket.setSendBufferSize(config.SOSendBufSize());
        }
        catch (final IOException e) {
            throw new PGErrorIO(e, "couldn't set socket options");
        }
    }

    public static PGSocketChannel connect(final Config config) {
        final int port = config.port();
        final String host = config.host();
        final InetSocketAddress address = new InetSocketAddress(host, port);
        try {
             SocketChannel channel = SocketChannel.open(address);
             Socket socket = channel.socket();
             setSocketOptions(socket, config);
            return new PGSocketChannel(address, socket);
        } catch (final IOException e) {
            throw new PGErrorIO(e, "cannot open socket, address: %s, cause: %s", address, e.getMessage());
        }
    }

    public InputStream getInputStream() {
        return IOTool.getInputStream(socket);
    }

    public OutputStream getOutputStream() {
        return IOTool.getOutputStream(socket);
    }

    public boolean isOpen() {
        return socket.isConnected();
    }

    public Certificate getPeerCertificate() {
        if (socket instanceof SSLSocket sslSocket) {
            return SSLTool.getPeerCertificate(sslSocket);
        } else {
            return null;
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    public PGIOChannel upgradeToSSL(final SSLContext sslContext) {
        final int port = address.getPort();
        final String host = address.getHostName();
        final SSLSocket sslSocket = SSLTool.connect(sslContext, socket, host, port, true);
        return new PGSocketChannel(address, sslSocket);
    }
}
