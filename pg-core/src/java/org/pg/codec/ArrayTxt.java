package org.pg.codec;

import clojure.lang.PersistentVector;
import org.pg.enums.OID;
import org.pg.error.PGError;

import java.io.IOException;
import java.io.Reader;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.*;

public final class ArrayTxt {

    public static String quoteElement(final String element) {
        final StringBuilder sb = new StringBuilder();
        final int len = element.length();
        char c;
        sb.append('"');
        for (int i = 0; i < len; i++) {
            c = element.charAt(i);
            sb.append(switch (c) {
                case '"' -> "\\\"";
                case '\\' -> "\\\\";
                default -> c;
            });
        }
        sb.append('"');
        return sb.toString();
    }

    public static String encode (final Object x, final OID oidArray, final CodecParams codecParams) {
        final OID oidEl = oidArray.toElementOID();
        Object val;
        if (x instanceof Iterable<?> i) {
            final Iterator<?> iterator = i.iterator();
            final StringBuilder sb = new StringBuilder();
            sb.append('{');
            while (iterator.hasNext()) {
                val = iterator.next();
                sb.append(encode(val, oidArray, codecParams));
                if (iterator.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append('}');
            return sb.toString();
        } else if (x == null) {
            return "NULL";
        } else {
            return quoteElement(EncoderTxt.encode(x, oidEl, codecParams));
        }
    }

    public static int read(final Reader reader) {
        try {
            return reader.read();
        } catch (IOException e) {
            throw new PGError(e, "cannot read from a reader");
        }
    }

    public static char readChar(final Reader reader) {
        int r;
        try {
            r = reader.read();
        } catch (IOException e) {
            throw new PGError(e, "cannot read from a reader");
        }
        if (r == -1) {
            throw new PGError("EOF reached");
        } else {
            return (char) r;
        }
    }

    public static void unread(final PushbackReader r, final char c) {
        try {
            r.unread(c);
        } catch (IOException e) {
            throw new PGError(e, "cannot unread a character: %s", c);
        }
    }

    public static String readQuotedString (final Reader reader) {
        readChar(reader);
        final StringBuilder sb = new StringBuilder();
        char c;
        while (true) {
            c = readChar(reader);
            switch (c) {
                case '"': {
                    return sb.toString();
                }
                case '\\': {
                    c = readChar(reader);
                    switch (c) {
                        case '\\', '"': {
                            sb.append(c);
                            break;
                        }
                        default: {
                            throw new PGError("unexpected \\ character");
                        }
                    }
                }
                default: {
                    sb.append(c);
                    break;
                }
            }
        }
    }

    public static boolean isNullLiteral(final String line) {
        return line.equalsIgnoreCase("null");
    }

    public static String readNonQuotedString(final PushbackReader reader) {
        final StringBuilder sb = new StringBuilder();
        char c;
        String line;
        while (true) {
            c = readChar(reader);
            switch (c) {
                case ',', '}': {
                    unread(reader, c);
                    line = sb.toString();
                    if (isNullLiteral(line)) {
                        return null;
                    } else {
                        return line;
                    }
                }
                default: {
                    sb.append(c);
                    break;
                }
            }
        }
    }

    public static Object decode(final String array, final OID arrayOid, final CodecParams codecParams) {
        final OID oidEl = arrayOid.toElementOID();
        final PushbackReader reader = new PushbackReader(new StringReader(array));
        final int[] dims = new int[16];
        int[] path;
        int pos = -1;
        int r;
        char c;
        Object obj;
        String buf;
        PersistentVector result = PersistentVector.EMPTY;

        while (true) {
            r = read(reader);
            if (r == -1) {
                return result;
            } else {
               c = (char) r;
            }
            switch (c) {
                case '{': {
                    pos++;
                    break;
                }
                case '}': {
                    dims[pos] = 0;
                    pos--;
                    break;
                }
                case ',': {
                    dims[pos] += 1;
                    break;
                }
                case '"': {
                    unread(reader, c);
                    buf = readQuotedString(reader);
                    obj = DecoderTxt.decode(buf, oidEl, codecParams);
                    path = Matrix.take(pos, dims);
                    result = Matrix.assocVecIn(result, path, obj);
                    break;
                }
                default: {
                    unread(reader, c);
                    buf = readNonQuotedString(reader);
                    if (buf == null) {
                        obj = null;
                    } else {
                        obj = DecoderTxt.decode(buf, oidEl, codecParams);
                    }
                    path = Matrix.take(pos, dims);
                    result = Matrix.assocVecIn(result, path, obj);
                    break;
                }
            }
        }
    }

    public static void main(String... args) {
        System.out.println(encode(
                PersistentVector.create("a'a\\aa", "b\"bb", null, "he'l{}lo"),
                OID._TEXT,
                CodecParams.standard()
        ));
        System.out.println(readQuotedString(new PushbackReader(new StringReader("\"aaa\\\\aaa\""))));
        System.out.println(readNonQuotedString(new PushbackReader(new StringReader("NuLL,"))));

        System.out.println(decode("{{1,2,3},{4,null,6}}", OID._INT2, CodecParams.standard()));
    }
}
