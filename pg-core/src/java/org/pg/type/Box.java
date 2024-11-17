package org.pg.type;

import clojure.lang.*;
import org.pg.error.PGError;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public record Box(Point p1, Point p2)
    implements IDeref, Counted, Indexed, Iterable<Point> {

    public static Box of(final double x1,
                         final double y1,
                         final double x2,
                         final double y2) {
        return new Box(Point.of(x1, y1), Point.of(x2, y2));
    }

    public ByteBuffer toByteBuffer() {
        final ByteBuffer bb = ByteBuffer.allocate(32);
        bb.putDouble(p1.x());
        bb.putDouble(p1.y());
        bb.putDouble(p2.x());
        bb.putDouble(p2.y());
        return bb;
    }

    public static Box fromByteBuffer(final ByteBuffer bb) {
        final double x1 = bb.getDouble();
        final double y1 = bb.getDouble();
        final double x2 = bb.getDouble();
        final double y2 = bb.getDouble();
        return Box.of(x1, y1, x2, y2);
    }

    public static Box fromString(final String text) {
        final String[] partsRaw = text.split("[(,)]");
        final List<String> partsClear = new ArrayList<>();
        for (String part: partsRaw) {
            String partClear = part.strip();
            if (!partClear.isEmpty()) {
                partsClear.add(partClear);
            }
        }
        if (partsClear.size() != 4) {
            throw new PGError("wrong box string: %s", text);
        }
        final double x1 = Double.parseDouble(partsClear.get(0));
        final double y1 = Double.parseDouble(partsClear.get(1));
        final double x2 = Double.parseDouble(partsClear.get(0));
        final double y2 = Double.parseDouble(partsClear.get(1));
        return Box.of(x1, y1, x2, y2);
    }

    @Override
    public String toString() {
        return p1.toString() + "," + p2.toString();
    }

    @Override
    public Object deref() {
        return PersistentVector.create(
                p1.deref(),
                p2.deref()
        );
    }

    @Override
    public int count() {
        return 2;
    }

    @Override
    public Object nth(int i) {
        return switch (i) {
            case 0 -> p1;
            case 1 -> p2;
            default -> throw new IndexOutOfBoundsException("index is out of range: " + i);
        };
    }

    @Override
    public Object nth(int i, Object notFound) {
        return switch (i) {
            case 0 -> p1;
            case 1 -> p2;
            default -> notFound;
        };
    }

    @Override
    public Iterator<Point> iterator() {
        return List.of(p1, p2).iterator();
    }
}
