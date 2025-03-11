package org.pg.codec;
import clojure.lang.Indexed;
import clojure.lang.PersistentVector;

import clojure.lang.RT;
import org.pg.error.PGError;
import org.pg.type.Matrix;
import org.pg.processor.IProcessor;

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
            switch (c) {
                case '\\', '"': {
                    sb.append('\\');
                    sb.append(c);
                    break;
                }
                default: {
                    sb.append(c);
                    break;
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static String encode (final Object x, final int oidEl, final CodecParams codecParams) {
        final IProcessor processor = null; // codecParams.getProcessor(oidEl);
        Object val;
        if (x instanceof Indexed) {
            final Iterator<?> iterator = RT.iter(x);
            final StringBuilder sb = new StringBuilder();
            sb.append('{');
            while (iterator.hasNext()) {
                val = iterator.next();
                sb.append(encode(val, oidEl, codecParams));
                if (iterator.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append('}');
            return sb.toString();
        } else if (x == null) {
            return "NULL";
        } else {
            return quoteElement(processor.encodeTxt(x, codecParams));
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
        readChar(reader); // skip leading "
        final StringBuilder sb = new StringBuilder();
        char c1, c2;
        while (true) {
            c1 = readChar(reader);
            switch (c1) {
                case '"': { // the trailing "
                    return sb.toString();
                }
                case '\\': {
                    c2 = readChar(reader);
                    switch (c2) {
                        case '\\', '"': {
                            sb.append(c2);
                            break;
                        }
                        default: {
                            throw new PGError("unexpected \\ character");
                        }
                    }
                    break;
                }
                default: {
                    sb.append(c1);
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

    public static Object decode(final String array, final int oidEl, final CodecParams codecParams) {
        final PushbackReader reader = new PushbackReader(new StringReader(array));
        final IProcessor processor = null; // codecParams.getProcessor(oidEl);
        final int limit = 16;
        final int[] pathMax = new int[limit];
        final int[] path = new int[limit];
        int pos = -1;
        int posMax = 0;
        int r;
        char c;
        Object obj;
        String buf;
        final List<Object> elements = new ArrayList<>();

        while (true) {
            r = read(reader);
            if (r == -1) {
                break;
            } else {
               c = (char) r;
            }
            switch (c) {
                case '{': {
                    pos++;
                    posMax = pos;
                    break;
                }
                case '}': {
                    path[pos] = 0;
                    pos--;
                    break;
                }
                case ',': {
                    path[pos] += 1;
                    if (pathMax[pos] < path[pos]) {
                        pathMax[pos] += 1;
                    }
                    break;
                }
                case '"': {
                    unread(reader, c);
                    buf = readQuotedString(reader);
                    obj = processor.decodeTxt(buf, codecParams);
                    elements.add(obj);
                    break;
                }
                default: {
                    unread(reader, c);
                    buf = readNonQuotedString(reader);
                    if (buf == null) {
                        obj = null;
                    } else {
                        obj = processor.decodeTxt(buf, codecParams);
                    }
                    elements.add(obj);
                    break;
                }
            }
        }

        final int[] dims = new int[posMax + 1];
        for (int i = 0; i < posMax + 1; i ++) {
            dims[i] = pathMax[i] + 1;
        }

        // plain array
        if (dims.length == 1) {
            return PersistentVector.create(elements);
        }

        // multi-dim array
        return Matrix.packElements(dims, elements);

    }

    public static void main(String... args) {
//        System.out.println(decode("{1,2,3}", OID._INT2, CodecParams.standard()));
//        System.out.println(decode("{{1,2,3},{1,2,3}}", OID._INT2, CodecParams.standard()));
//        System.out.println(decode("{{{1,2,3},{4,5,6}},{{1,2,3},{4,5,6}}}", OID._INT2, CodecParams.standard()));
//        System.out.println(quoteElement("{\"foo\": 123}"));
//        System.out.println(encode(
//                PersistentVector.create("a'a\\aa", "b\"bb", null, "hi \\\\test"),
//                OID._TEXT,
//                CodecParams.standard()
//        ));
//        System.out.println(readQuotedString(new PushbackReader(new StringReader("\"aaa\\\"bbb\""))));
//        System.out.println(readNonQuotedString(new PushbackReader(new StringReader("NuLL,"))));
//
//        System.out.println(decode("{{1,2,3},{4,null,6}}", OID._INT2, CodecParams.standard()));
    }
}
