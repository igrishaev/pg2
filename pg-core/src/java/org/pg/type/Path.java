package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;
import org.pg.error.PGError;
import org.pg.util.GeomTool;
import org.pg.util.StrTool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record Path(List<Point> points, boolean isClosed) {

    public static Path of(final List<Point> points, final boolean isClosed) {
        return new Path(points, isClosed);
    }

    public static Path of(final List<Point> points) {
        return of(points, true);
    }

    @SuppressWarnings("unused")
    public static Path fromObject(final Object x) {
        if (x instanceof Path p) {
            return p;
        } else if (x instanceof Map<?,?> m) {
            return Path.fromMap(m);
        } else if (x instanceof Iterable<?> iter) {
            return Path.fromList(iter);
        } else if (x instanceof String s) {
            return Path.fromSQL(s);
        } else if (x instanceof ByteBuffer bb) {
            return Path.fromByteBuffer(bb);
        } else {
            throw new PGError("wrong path input: %s", x);
        }
    }

    public static Path fromMap(final Map<?,?> map) {
        Boolean isClosed = (Boolean) map.get(KW.closed_QMARK);
        if (isClosed == null) {
            isClosed = true;
        }
        final Iterable<?> pointsRaw = (Iterable<?>) map.get(KW.points);
        final List<Point> points = GeomTool.parsePoints(pointsRaw);
        return Path.of(points, isClosed);
    }

    public static Path fromList(final Iterable<?> iterable) {
        return fromList(iterable, true);
    }

    public static Path fromList(final Iterable<?> iterable, final boolean isClosed) {
        final List<Point> points =  GeomTool.parsePoints(iterable);
        return Path.of(points, isClosed);
    }

    public static Path fromByteBuffer(final ByteBuffer bb) {
        final byte byteClosed = bb.get();
        final boolean isClosed = byteClosed == (byte) 1;
        final int len = bb.getInt();
        final List<Point> points = new ArrayList<>(len);
        Point point;
        for (int i = 0; i < len; i++) {
            point = Point.fromByteBuffer(bb);
            points.add(point);
        }
        return Path.of(points, isClosed);
    }

    public ByteBuffer toByteBuffer() {
        final int len = points.size();
        final ByteBuffer bb = ByteBuffer.allocate(1 + 4 + len * 16);
        bb.put(isClosed ? (byte) 1 : (byte) 0);
        bb.putInt(len);
        ByteBuffer bbPoint;
        for (int i = 0; i < len; i++) {
            bbPoint = points.get(i).toByteBuffer();
            bbPoint.rewind();
            bb.put(bbPoint);
        }
        return bb;
    }

    public String toSQL() {
        final String inner = String.join(",", points.stream().map(Point::toSQL).toList());
        return (isClosed ? "(" : "[") + inner + (isClosed ? ")" : "]");
    }

    public static Path fromSQL(final String text) {
        final boolean isClosed = text.strip().startsWith("(");
        final List<String> parts = StrTool.split(text, "[\\[(,)\\]]");
        final List<Point> points = GeomTool.parsePoints(parts);
        return Path.of(points, isClosed);
    }

    public IPersistentCollection toClojure() {
        return PersistentHashMap.create(
                KW.closed_QMARK, isClosed,
                KW.points, PersistentVector.create(
                        points.stream().map(Point::toClojure).toList()
                )
        );
    }
}
