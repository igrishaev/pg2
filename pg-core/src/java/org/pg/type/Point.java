package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;
import org.pg.error.PGError;
import org.pg.util.NumTool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public record Point (double x, double y)
        implements Counted, Indexed, ILookup, IDeref, Iterable<Double> {

    public ByteBuffer toByteBuffer() {
        final ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putDouble(x);
        bb.putDouble(y);
        return bb;
    }

    public static Point fromByteBuffer(final ByteBuffer bb) {
        final double x = bb.getDouble();
        final double y = bb.getDouble();
        return new org.pg.type.Point(x, y);
    }

    public static Point fromMap(final Map<?,?> map) {
        final double x = NumTool.toDouble(map.get(KW.x));
        final double y = NumTool.toDouble(map.get(KW.y));
        return new Point(x, y);
    }

    public static Point fromList(final List<?> list) {
        final double x = NumTool.toDouble(list.get(0));
        final double y = NumTool.toDouble(list.get(1));
        return new Point(x, y);
    }

    public static Point fromString(final String text) {
        final String[] partsRaw = text.split("[(,)]");
        final List<String> partsClear = new ArrayList<>();
        for (String part: partsRaw) {
            String partClear = part.strip();
            if (!partClear.isEmpty()) {
                partsClear.add(partClear);
            }
        }
        if (partsClear.size() != 2) {
            throw new PGError("wrong point string: %s", text);
        }
        final double x = Double.parseDouble(partsClear.get(0));
        final double y = Double.parseDouble(partsClear.get(1));
        return new Point(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    @Override
    public Object nth(final int i) {
        return switch (i) {
            case 0 -> x;
            case 1 -> y;
            default -> throw new IndexOutOfBoundsException("index is out of range: " + i);
        };
    }

    @Override
    public Object nth(final int i, final Object notFound) {
        return switch (i) {
            case 0 -> x;
            case 1 -> y;
            default -> notFound;
        };
    }

    @Override
    public int count() {
        return 2;
    }

    @Override
    public Object valAt(final Object key) {
        return valAt(key, null);
    }

    @Override
    public Object valAt(final Object key, final Object notFound) {
        if (key instanceof Keyword kw) {
            if (kw == KW.x) {
                return x;
            } else if (kw == KW.y) {
                return y;
            }
        }
        return notFound;
    }

    @Override
    public Object deref() {
        return PersistentHashMap.create(KW.x, x, KW.y, y);
    }

    public static void main(String... args) {
        System.out.println(Point.fromString("(1.23342 , -3.23234)"));
    }

    @Override
    public Iterator<Double> iterator() {
        return List.of(x, y).iterator();
    }
}
