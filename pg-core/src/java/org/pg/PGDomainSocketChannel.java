package org.pg;

import org.pg.error.PGErrorIO;
import org.pg.util.IOTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;

public class PGDomainSocketChannel implements PGIOChannel {
    private SocketChannel channel;

    protected PGDomainSocketChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public static PGDomainSocketChannel connect(UnixDomainSocketAddress address) {
        try {
            final SocketChannel channel = SocketChannel.open(address);
            return new PGDomainSocketChannel(channel);
        } catch (IOException e) {
            throw new PGErrorIO(e, "cannot open socket, address: %s, cause: %s", address, e.getMessage());
        }
    }

    public InputStream getInputStream() {
        return Channels.newInputStream(channel);
    }

    public OutputStream getOutputStream() {
        return Channels.newOutputStream(channel);
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    public Certificate getPeerCertificate() {
        return null;
    }

    public void close() throws IOException {
        channel.close();
    }

    public PGIOChannel upgradeToSSL(SSLContext sslContext) {
        throw new UnsupportedOperationException("Unable to upgrade domain socket to SSL");
    }
}
