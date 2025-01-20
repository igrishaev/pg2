package org.pg;

import org.pg.error.PGError;
import org.pg.util.IOTool;
import org.pg.util.SSLTool;
import org.pg.util.SocketTool;

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
    private InetSocketAddress address;
    private Socket socket;

    protected PGSocketChannel(InetSocketAddress address, Socket socket) {
        this.address = address;
        this.socket = socket;
    }

    public static PGSocketChannel connect(Config config) {
        final int port = config.port();
        final String host = config.host();
        final InetSocketAddress address = new InetSocketAddress(host, port);
        try {
             SocketChannel channel = SocketChannel.open(address);
             Socket socket = channel.socket();
             SocketTool.setSocketOptions(socket, config);
            return new PGSocketChannel(address, socket);
        } catch (IOException e) {
            throw new PGError(e, "cannot open socket, address: %s, cause: %s", address, e.getMessage());
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

    public PGIOChannel upgradeToSSL(SSLContext sslContext) {
        Integer port = address.getPort();
        String host = address.getHostName();
        SSLSocket sslSocket = SSLTool.connect(sslContext, socket, host, port, true);
        return new PGSocketChannel(address, sslSocket);
    }
}
