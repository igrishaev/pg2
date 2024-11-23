package org.pg.type;

import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;
import org.pg.clojure.KW;
import org.pg.util.GeomTool;
import org.pg.util.StrTool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record Path(boolean isClosed, List<Point> points) {

    public static Path of(final boolean isClosed, final List<Point> points) {
        return new Path(isClosed, points);
    }

    public static Path fromMap(final Map<?,?> map) {
        Boolean isClosed = (Boolean) map.get(KW.isClosed);
        if (isClosed == null) {
            isClosed = true;
        }
        final Iterable<?> pointsRaw = (Iterable<?>) map.get(KW.points);
        final List<Point> points = GeomTool.parsePoints(pointsRaw);
        return Path.of(isClosed, points);
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
        return Path.of(isClosed, points);
    }

    public ByteBuffer toByteBuffer() {
        final int len = points.size();
        final ByteBuffer bb = ByteBuffer.allocate(1 + 4 + len * 16);
        bb.put(isClosed ? (byte) 1 : (byte) 0);
        bb.putInt(len);
        for (int i = 0; i < len; i++) {
            bb.put(points.get(i).toByteBuffer());
        }
        return bb;
    }

    public String toSQL() {
        final String inner = String.join(",", points.stream().map(Point::toString).toList());
        return (isClosed ? "(" : "[") + inner + (isClosed ? ")" : "]");
    }

    public static Path fromSQL(final String text) {
        final boolean isClosed = text.strip().startsWith("(");
        final List<String> parts = StrTool.split(text, "[[(,)]]");
        final List<Point> points = GeomTool.parsePoints(parts);
        return Path.of(isClosed, points);
    }

    public IPersistentCollection toClojure() {
        return PersistentHashMap.create(
                KW.isClosed, isClosed,
                KW.points, PersistentVector.create(
                        points.stream().map(Point::toClojure).toList()
                )
        );
    }
}
