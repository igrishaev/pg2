package org.pg.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class NumericBin {

    private static final int DECIMAL_DIGITS = 4;
    private static final BigInteger TEN_THOUSAND = new BigInteger("10000");

    private final static int NUMERIC_POS = 0x0000;
    private final static int NUMERIC_NEG = 0x4000;
    private final static int NUMERIC_NAN = 0xC000;

    public static ByteBuffer encode(final BigDecimal value) {
        // Number of fractional digits:
        final int fractionDigits = value.scale();

        // Number of Fraction Groups:
        final int fractionGroups = fractionDigits > 0 ? (fractionDigits + 3) / 4 : 0;

        final List<Integer> digits = digits(value);

        final int bbLen = 8 + (2 * digits.size());
        final ByteBuffer bb = ByteBuffer.allocate(bbLen);

        bb.putShort((short) digits.size());
        bb.putShort((short) (digits.size() - fractionGroups - 1));
        bb.putShort((short) (value.signum() == 1 ? NUMERIC_POS : NUMERIC_NEG));
        bb.putShort((short) (fractionDigits > 0 ? fractionDigits : 0));

        for (int pos = digits.size() - 1; pos >= 0; pos--) {
            final int valueToWrite = digits.get(pos);
            bb.putShort((short) valueToWrite);
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

    // Inspired by implementation here:
    // https://github.com/PgBulkInsert/PgBulkInsert/blob/master/PgBulkInsert/src/main/java/de/bytefish/pgbulkinsert/pgsql/handlers/BigDecimalValueHandler.java
    private static List<Integer> digits(final BigDecimal value) {
        BigInteger unscaledValue = value.unscaledValue();

        if (value.signum() == -1) {
            unscaledValue = unscaledValue.negate();
        }

        final List<Integer> digits = new ArrayList<>();

        if (value.scale() > 0) {
            // The scale needs to be a multiple of 4:
            int scaleRemainder = value.scale() % 4;

            // Scale the first value:
            if (scaleRemainder != 0) {
                final BigInteger[] result = unscaledValue.divideAndRemainder(BigInteger.TEN.pow(scaleRemainder));
                final int digit = result[1].intValue() * (int) Math.pow(10, DECIMAL_DIGITS - scaleRemainder);
                digits.add(digit);
                unscaledValue = result[0];
            }

            while (!unscaledValue.equals(BigInteger.ZERO)) {
                final BigInteger[] result = unscaledValue.divideAndRemainder(TEN_THOUSAND);
                digits.add(result[1].intValue());
                unscaledValue = result[0];
            }
        } else {
            BigInteger originalValue = unscaledValue.multiply(BigInteger.TEN.pow(Math.abs(value.scale())));
            while (!originalValue.equals(BigInteger.ZERO)) {
                final BigInteger[] result = originalValue.divideAndRemainder(TEN_THOUSAND);
                digits.add(result[1].intValue());
                originalValue = result[0];
            }
        }

        return digits;
    }

    public static void main (final String[] args) {
        String arg = args.length > 0 ? args[0] : "1";
        BigDecimal num = new BigDecimal(arg);
        final ByteBuffer bb = encode(num);
        bb.rewind();
        System.out.println(decode(bb));
        //System.out.println(java.util.Arrays.toString(encode(num).array()));
    }

}
