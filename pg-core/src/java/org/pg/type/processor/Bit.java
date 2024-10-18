package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;

import java.nio.ByteBuffer;

public class Bit extends AProcessor {

    public static int oid = OID.BIT;
    private static final byte[] masks = {-128, 64, 32, 16, 8, 4, 2, 1};

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            final int bitLen = s.length();
            final int padLen = s.length() % 8;
            if (padLen != 0) {
                s = s + "0".repeat(8 - padLen);
            }
            final int byteLen = s.length() / 8;
            final ByteBuffer bb = ByteBuffer.allocate(4 + byteLen);
            bb.putInt(bitLen);
            String byteString;
            byte b;
            for (int i = 0; i < byteLen; i++) {
                byteString = s.substring(i * 8, (i + 1) * 8);
                b = Short.valueOf(byteString, 2).byteValue();
                bb.put(b);
            }
            return bb;
        } else if (x instanceof byte[] ba) {
            final int bitLen = ba.length * 8;
            final ByteBuffer bb = ByteBuffer.allocate(4 + ba.length);
            bb.putInt(bitLen);
            bb.put(ba);
            return bb;
        } else  {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof String s) {
            return s;
        } else if (x instanceof byte[] ba) {
            final int bitLen = ba.length * 8;
            final StringBuilder sb = new StringBuilder(bitLen);
            for (byte b: ba) {
                for (byte m: masks) {
                    if ((b & m) == m) {
                        sb.append('1');
                    } else {
                        sb.append('0');
                    }
                }
            }
            return sb.substring(0, bitLen);
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public String decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        final int bitLen = bb.getInt();
        int byteLen;
        if (bitLen % 8 == 0) {
            byteLen = bitLen / 8;
        } else {
            byteLen = bitLen / 8 + 1;
        }
        final StringBuilder sb = new StringBuilder(bitLen);
        byte b;
        for (int i = 0; i < byteLen; i++) {
            b = bb.get();
            for (byte m: masks) {
                if ((b & m) == m) {
                    sb.append('1');
                } else {
                    sb.append('0');
                }
            }
        }
        return sb.substring(0, bitLen);
    }

    @Override
    public String decodeTxt(final String text, final CodecParams codecParams) {
        return text;
    }
}
