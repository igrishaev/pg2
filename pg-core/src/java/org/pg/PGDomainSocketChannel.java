package org.pg;

import org.pg.error.PGErrorIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;

/*
A Unix domain socket backend. Streams over a blocking SocketChannel
ignore read timeouts, so the channel is non-blocking and reads wait
on a Selector instead. This makes setReadTimeout behave as it does
for TCP sockets: a read that times out throws SocketTimeoutException.
 */
public class PGDomainSocketChannel implements PGIOChannel {
    private final SocketChannel channel;
    private final UnixDomainSocketAddress address;
    private final Selector readSelector;
    private final Selector writeSelector;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private volatile int readTimeoutMs;

    protected PGDomainSocketChannel(final UnixDomainSocketAddress address,
                                    final SocketChannel channel,
                                    final int readTimeoutMs) throws IOException {
        this.address = address;
        this.channel = channel;
        this.readTimeoutMs = readTimeoutMs;
        channel.configureBlocking(false);
        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();
        channel.register(readSelector, SelectionKey.OP_READ);
        channel.register(writeSelector, SelectionKey.OP_WRITE);
        this.inputStream = new SelectorInputStream();
        this.outputStream = new SelectorOutputStream();
    }

    public static PGDomainSocketChannel connect(final UnixDomainSocketAddress address, final Config config) {
        SocketChannel channel = null;
        try {
            channel = SocketChannel.open(address);
            // Reads during connection setup are bounded by the connect
            // timeout; the connection switches to SOTimeout afterwards.
            return new PGDomainSocketChannel(address, channel, config.connectTimeout());
        } catch (final IOException e) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (final IOException ignored) {
                    // keep the original error
                }
            }
            throw new PGErrorIO(e, "cannot open socket, address: %s, cause: %s", address, e.getMessage());
        }
    }

    @Override
    public String represent() {
        return address.toString();
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void setReadTimeout(final int timeoutMs) {
        this.readTimeoutMs = timeoutMs;
    }

    @Override
    public Certificate getPeerCertificate() {
        return null;
    }

    public void close() throws IOException {
        try {
            readSelector.close();
            writeSelector.close();
        } finally {
            channel.close();
        }
    }

    @Override
    public PGIOChannel upgradeToSSL(final SSLContext sslContext) {
        throw new UnsupportedOperationException("Unable to upgrade domain socket to SSL");
    }

    /*
    Wait until the selector reports the channel ready, up to
    timeoutMs (0 means forever). Returns false on timeout.
     */
    private static boolean await(final Selector selector, final long timeoutMs) throws IOException {
        final int ready = timeoutMs == 0 ? selector.select() : selector.select(timeoutMs);
        selector.selectedKeys().clear();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("interrupted while waiting on " + selector);
        }
        return ready > 0;
    }

    private final class SelectorInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            final byte[] b = new byte[1];
            final int r = read(b, 0, 1);
            return r == -1 ? -1 : b[0] & 0xFF;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            final ByteBuffer dst = ByteBuffer.wrap(b, off, len);
            final int timeoutMs = readTimeoutMs;
            final long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
            while (true) {
                final int n = channel.read(dst);
                if (n != 0) {
                    return n;
                }
                long waitMs = 0;
                if (timeoutMs > 0) {
                    waitMs = (deadline - System.nanoTime()) / 1_000_000L;
                    if (waitMs <= 0) {
                        throw new SocketTimeoutException("Read timed out");
                    }
                }
                await(readSelector, waitMs);
            }
        }

        /*
        A non-blocking channel cannot report how many bytes are
        waiting, so report 1 when the channel is readable. That
        includes end of stream, so a subsequent read notices when
        the server has closed the connection.
         */
        @Override
        public int available() throws IOException {
            if (!channel.isOpen()) {
                throw new ClosedChannelException();
            }
            final int ready = readSelector.selectNow();
            readSelector.selectedKeys().clear();
            return ready > 0 ? 1 : 0;
        }

        @Override
        public void close() throws IOException {
            PGDomainSocketChannel.this.close();
        }
    }

    private final class SelectorOutputStream extends OutputStream {

        @Override
        public void write(final int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            final ByteBuffer src = ByteBuffer.wrap(b, off, len);
            while (src.hasRemaining()) {
                if (channel.write(src) == 0) {
                    // The send buffer is full; wait until it drains.
                    await(writeSelector, 0);
                }
            }
        }

        @Override
        public void close() throws IOException {
            PGDomainSocketChannel.this.close();
        }
    }
}
