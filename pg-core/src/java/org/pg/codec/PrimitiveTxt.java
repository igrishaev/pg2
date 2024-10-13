package org.pg.codec;

import org.pg.error.PGError;
import org.pg.util.HexTool;

import java.nio.ByteBuffer;

public class PrimitiveTxt {

    public static String encodeBool(final boolean b) {
        return b ? "t" : "f";
    }

    public static boolean decodeBool(final String string) {
        return switch (string) {
            case "t" -> true;
            case "f" -> false;
            default -> throw new PGError("wrong boolean value: %s", string);
        };
    }

    public static String encodeBytea(final byte[] ba) {
        return HexTool.formatHex(ba, "\\x");
    }

    public static byte[] decodeBytea(final String string) {
        return HexTool.parseHex(string, 2, string.length());
    }

}
