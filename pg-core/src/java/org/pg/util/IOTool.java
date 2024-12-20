package org.pg.util;

import org.pg.error.PGError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class IOTool {

    public static void close (final InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new PGError(e, "cannot close ");
        }
    }

    public static void skip (final InputStream inputStream, final int len) {
        try {
            inputStream.readNBytes(len);
        }
        catch (IOException e) {
            throw new PGError("Could not skip %s byte(s)", len);
        }
    }

    public static byte[] readNBytes (final InputStream inputStream, final int len) {
        try {
            return inputStream.readNBytes(len);
        }
        catch (IOException e) {
            throw new PGError("Could not read %s byte(s)", len);
        }
    }

    public static int read (
            final InputStream inputStream,
            final byte[] buf
    ) {
        try {
            return inputStream.read(buf);
        } catch (IOException e) {
            throw new PGError(e, "cannot read from the input stream");
        }
    }

    public static int read (
            final InputStream inputStream,
            final byte[] buf,
            final int offset,
            int len
    ) {
        try {
            return inputStream.read(buf, offset, len);
        } catch (IOException e) {
            throw new PGError(e, "cannot read from the input stream");
        }
    }

    public static int read (
            final InputStream inputStream
    ) {
        try {
            return inputStream.read();
        } catch (IOException e) {
            throw new PGError(e, "cannot read from the input stream");
        }
    }

    public static void write(final OutputStream outputStream, final byte[] buf) {
        try {
            outputStream.write(buf);
        } catch (IOException e) {
            throw new PGError(e, "cannot write a byte array into an output stream");
        }
    }

    public static void flush(final OutputStream outputStream) {
        try {
            outputStream.flush();
        } catch (IOException e) {
            throw new PGError(e, "cannot flush an output stream");
        }
    }

    public static void write (
            final OutputStream outputStream,
            final byte[] buf,
            final int offset,
            final int len
    ) {
        try {
            outputStream.write(buf, offset, len);
        } catch (IOException e) {
            throw new PGError(
                    e,
                    "cannot write a byte array into an output stream, offset: %s, len: %s",
                    offset, len
            );
        }
    }

    public static InputStream getInputStream(final Socket socket) {
        try {
            return socket.getInputStream();
        }
        catch (IOException e) {
            throw new PGError(
                    e,
                    "cannot get an input stream from a socket"
            );
        }
    }

    public static OutputStream getOutputStream(final Socket socket) {
        try {
            return socket.getOutputStream();
        }
        catch (IOException e) {
            throw new PGError(
                    e,
                    "cannot get an output stream from a socket"
            );
        }
    }
}
