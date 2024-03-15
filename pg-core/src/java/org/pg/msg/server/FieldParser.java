package org.pg.msg.server;

import org.pg.error.PGError;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class FieldParser {

    public static String parseTag (final char tag) {
        return switch (tag) {
            case 'S' -> "severity";
            case 'V' -> "verbosity";
            case 'C' -> "code";
            case 'M' -> "message";
            case 'D' -> "detail";
            case 'H' -> "hint";
            case 'P' -> "position";
            case 'p' -> "position-internal";
            case 'q' -> "query";
            case 'W' -> "stacktrace";
            case 's' -> "schema";
            case 't' -> "table";
            case 'c' -> "column";
            case 'd' -> "datatype";
            case 'n' -> "constraint";
            case 'F' -> "file";
            case 'L' -> "line";
            case 'R' -> "function";
            default -> throw new PGError("unknown tag: %s", tag);
        };
    }

    public static Map<String, String> parseFields(
            final ByteBuffer buf,
            final Charset charset
    ) {
        final HashMap<String, String> fields = new HashMap<>();
        while (true) {
            final byte tag = buf.get();
            if (tag == 0) {
                break;
            }
            else {
                final String field = parseTag((char)tag);
                final String message = BBTool.getCString(buf, charset);
                fields.put(field, message);
            };
        };
        return fields;
    }
}
