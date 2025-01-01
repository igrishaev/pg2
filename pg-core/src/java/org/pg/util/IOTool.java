package org.pg.util;

import org.pg.error.PGError;

import java.io.*;
import java.net.Socket;

public final class IOTool {

    public static int available (final InputStream inputStream) {
        try {
            return inputStream.available();
        } catch (IOException e) {
            throw new PGError(e, "cannot get bytes available");
        }
    }

    public static void close (final InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new PGError(e, "cannot close input stream");
        }
    }

    public static void close (final OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new PGError(e, "cannot close output stream");
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

    public static BufferedInputStream wrapBuf(final InputStream in, final int size) {
        if (in instanceof BufferedInputStream b) {
            return b;
        } else {
            return new BufferedInputStream(in, size);
        }
    }

    public static BufferedOutputStream wrapBuf(final OutputStream out, final int size) {
        if (out instanceof BufferedOutputStream b) {
            return b;
        } else {
            return new BufferedOutputStream(out, size);
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
