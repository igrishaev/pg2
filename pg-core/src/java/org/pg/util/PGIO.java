package org.pg.util;

import org.pg.error.PGError;

import java.io.*;

public final class PGIO {

    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;

    public PGIO (final InputStream in, final OutputStream out) {
        dataInputStream = new DataInputStream(in);
        dataOutputStream = new DataOutputStream(out);
    }

    public char readChar() {
        try {
            return (char) dataInputStream.readUnsignedByte();
        } catch (IOException e) {
            throw new PGError(e, "Error reading char");
        }
    }

    public int readInt() {
        try {
            return dataInputStream.readInt();
        } catch (IOException e) {
            throw new PGError(e, "Error reading int");
        }
    }

    public void skip(final long n) {
        try {
            dataInputStream.skip(n);
        } catch (IOException e) {
            throw new PGError(e, "Error skipping, n: %s", n);
        }
    }

    public void readFully(final byte[] b) {
        try {
            dataInputStream.readFully(b);
        } catch (IOException e) {
            throw new PGError(e, "Error reading bytes");
        }
    }

//    public String readCString() {
//        int b;
//        final ByteArrayOutputStream out = new ByteArrayOutputStream();
//        while (true) {
//            b = dataInputStream.read();
//            if (b == -1) {
//
//            }
//            else if (b == '\n') {}
//
//            out.write(b);
//        }
//
//    }



}
