package org.pg;

import org.pg.error.PGErrorIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;

public class PGDomainSocketChannel implements PGIOChannel {
    private final SocketChannel channel;

    protected PGDomainSocketChannel(final SocketChannel channel) {
        this.channel = channel;
    }

    public static PGDomainSocketChannel connect(final UnixDomainSocketAddress address) {
        try {
            final SocketChannel channel = SocketChannel.open(address);
            return new PGDomainSocketChannel(channel);
        } catch (final IOException e) {
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

    public PGIOChannel upgradeToSSL(final SSLContext sslContext) {
        throw new UnsupportedOperationException("Unable to upgrade domain socket to SSL");
    }
}
