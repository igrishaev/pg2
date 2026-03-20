package org.pg.util;

import org.pg.error.PGErrorIO;

import java.io.*;
import java.net.Socket;

public final class IOTool {

    public static void close (final InputStream inputStream) {
        try {
            inputStream.close();
        } catch (final IOException e) {
            throw new PGErrorIO(e, "cannot close input stream, cause: %s", e.getMessage());
        }
    }

    public static void close (final OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (final IOException e) {
            throw new PGErrorIO(e, "cannot close output stream, cause: %s", e.getMessage());
        }
    }

    public static BufferedInputStream wrapBuf(final InputStream in, final int size) {
        if (in instanceof final BufferedInputStream b) {
            return b;
        } else {
            return new BufferedInputStream(in, size);
        }
    }

    public static BufferedOutputStream wrapBuf(final OutputStream out, final int size) {
        if (out instanceof final BufferedOutputStream b) {
            return b;
        } else {
            return new BufferedOutputStream(out, size);
        }
    }

    public static void write(final OutputStream outputStream, final byte[] buf) {
        try {
            outputStream.write(buf);
        } catch (final IOException e) {
            throw new PGErrorIO(e, "cannot write a byte array into an output stream");
        }
    }

}
