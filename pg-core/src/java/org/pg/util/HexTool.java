package org.pg.util;

import org.pg.error.PGError;
import java.util.Arrays;

public final class HexTool {


    public static String formatHex(final byte[] input) {
        return formatHex(input, "");
    }

    public static String formatHex(final byte[] input, final String prefix) {
        final int len = prefix.length() + 2 * input.length;
        final StringBuilder sb = new StringBuilder(len);
        sb.append(prefix);
        for (final byte b: input) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte charToByte (final char c) {
        return switch (c) {
            case '0' -> 0;
            case '1' -> 1;
            case '2' -> 2;
            case '3' -> 3;
            case '4' -> 4;
            case '5' -> 5;
            case '6' -> 6;
            case '7' -> 7;
            case '8' -> 8;
            case '9' -> 9;
            case 'a', 'A' -> 10;
            case 'b', 'B' -> 11;
            case 'c', 'C' -> 12;
            case 'd', 'D' -> 13;
            case 'e', 'E' -> 14;
            case 'f', 'F' -> 15;
            default -> throw new PGError("cannot hex-parse a character: %s", c);
        };
    }

    public static byte[] parseHex(final String input) {
        return parseHex(input, 0, input.length());
    }

    public static byte[] parseHex(final String input, final int fromIndex) {
        return parseHex(input, fromIndex, input.length());
    }

    public static byte[] parseHex(final String input, final int fromIndex, final int toIndex) {
        final int size = (toIndex - fromIndex) / 2;
        final byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            final int offset = fromIndex + i * 2;
            final char cHigh = input.charAt(offset);
            final char cLow = input.charAt(offset + 1);
            final byte bHigh = charToByte(cHigh);
            final byte bLow = charToByte(cLow);
            result[i] = (byte) (bHigh * 16 + bLow);
        }
        return result;
    }

    public static void main(final String[] args) {
        final byte[] result = parseHex("\\x10FaaEc3", 2);
        System.out.println(Arrays.toString(result));
        System.out.println(formatHex(result, "\\x"));
    }

}
