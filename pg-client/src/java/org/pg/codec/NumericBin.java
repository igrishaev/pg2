package org.pg.codec;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;

public class NumericBin {

    private final static int NUMERIC_POS = 0x0000;
    private final static int NUMERIC_NEG = 0x4000;
    private final static int NUMERIC_NAN = 0xC000;

    public static ByteBuffer encode(final BigDecimal value) {
        final int scale = value.scale();
        final String[] parts = value.toPlainString().split("\\.");
        final String lead = parts[0];
        final boolean isNegative = lead.startsWith("-");
        final String hi = isNegative ? lead.substring(1) : lead;
        final String lo = parts.length > 1 ? parts[1] : "";
        final int hiLen = hi.length();
        final int loLen = lo.length();
        final int sign = isNegative ? NUMERIC_NEG : NUMERIC_POS;
        final int weight = hiLen / 4;
        final int padLeft = 4 - hiLen % 4;
        final int padRight = 4 - (padLeft + hiLen + loLen) % 4;
        final String digitsStr = "0".repeat(padLeft) + hi + lo + "0".repeat(padRight);
        final int digitsNum = digitsStr.length() / 4;
        final short[] shorts = new short[digitsNum];

        for (int i = 0; i < digitsNum; i++) {
            int idxStart = i * 4;
            int idxEnd = idxStart + 4;
            String part = digitsStr.substring(idxStart, idxEnd);
            shorts[i] = Short.parseShort(part);
        }

        final int bbLen = 8 + 2 * digitsNum;
        final ByteBuffer bb = ByteBuffer.allocate(bbLen);
        bb.putShort((short) digitsNum);
        bb.putShort((short) weight);
        bb.putShort((short) sign);
        bb.putShort((short) scale);

        for (final Short sh: shorts) {
            bb.putShort(sh);
        }

        return bb;
    }

    public static BigDecimal decode(final ByteBuffer bb) {
        final short digitsNum = bb.getShort();

        if (digitsNum == 0) {
            return BigDecimal.ZERO;
        }

        final short weight = bb.getShort();
        final short sigh = bb.getShort();
        final short scale = bb.getShort();
        final short[] shorts = new short[digitsNum];
        for (short i = 0; i < digitsNum; i++) {
            shorts[i] = bb.getShort();
        }
        final StringBuilder sb = new StringBuilder();
        if (sigh != 0) {
            sb.append("-");
        }
        sb.append("0.");
        for (short i = 0; i < digitsNum; i++) {
            sb.append(String.format("%04d", shorts[i]));
        }
        return new BigDecimal(sb.toString())
                .movePointRight(4 * (weight + 1))
                .setScale(scale, RoundingMode.DOWN);
    }

    public static void main (final String[] args) {
        final ByteBuffer bb = encode(new BigDecimal("1"));
        bb.rewind();
        // System.out.println(Arrays.toString(encodeBin(new BigDecimal("1")).array()));
        System.out.println(decode(bb));
    }

}
