package org.pg.type.processor.pgvector;

import org.pg.codec.CodecParams;
import org.pg.error.PGError;
import org.pg.type.processor.AProcessor;

import java.nio.ByteBuffer;

public class Bit extends AProcessor {

    private static final byte[] masks = {-128, 64, 32, 16, 8, 4, 2, 1};

    @Override
    public ByteBuffer encodeBin(final Object x , final CodecParams codecParams) {
        if (x instanceof String s) {
            if (s.length() % 8 != 0) {
                throw new PGError("binary string doesn't divide on 8: %s", s);
            }
            final int bytes = s.length() / 8;
            final ByteBuffer bb = ByteBuffer.allocate(4 + bytes);
            bb.putInt(bytes);
            String byteString;
            byte b;
            for (int i = 0; i < bytes; i++) {
                byteString = s.substring(i * 8, i * 8 + 8);
                b = Byte.valueOf(byteString, 2);
                bb.put(b);
            }
            return bb;
        } else if (x instanceof byte[] ba) {
            final ByteBuffer bb = ByteBuffer.allocate(4 + ba.length);
            bb.putInt(ba.length);
            bb.put(ba);
            return bb;
        } else {
            return binEncodingError(x);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else if (x instanceof byte[] ba) {
            final int len = ba.length * 8;
            final StringBuilder sb = new StringBuilder(len);
            for (byte b: ba) {
                for (byte m: masks) {
                    if ((b & m) == m) {
                        sb.append('1');
                    } else {
                        sb.append('0');
                    }
                }
            }
            return sb.toString();
        } else {
            return txtEncodingError(x);
        }
    }

    @Override
    public String decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        final int bits = bb.getInt();
        final StringBuilder sb = new StringBuilder(bits);
        final int len = bits / 8;
        byte b;
        for (int i = 0; i < len; i++) {
            b = bb.get();
            for (byte m: masks) {
                if ((b & m) == m) {
                    sb.append('1');
                } else {
                    sb.append('0');
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String decodeTxt(final String text, final CodecParams codecParams) {
        return text;
    }
}
