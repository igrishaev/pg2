package org.pg.type;

import clojure.lang.Keyword;
import clojure.lang.PersistentVector;
import org.pg.clojure.KW;
import org.pg.error.PGError;
import org.pg.util.NumTool;
import org.pg.util.StrTool;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public record LineSegment(Point p1, Point p2) {

    public static LineSegment of(final Point p1, final Point p2) {
        return new LineSegment(p1, p2);
    }

    public static LineSegment of(final double x1,
                         final double y1,
                         final double x2,
                         final double y2) {
        return new LineSegment(Point.of(x1, y1), Point.of(x2, y2));
    }

    public ByteBuffer toByteBuffer() {
        final ByteBuffer bb = ByteBuffer.allocate(32);
        bb.putDouble(p1.x());
        bb.putDouble(p1.y());
        bb.putDouble(p2.x());
        bb.putDouble(p2.y());
        return bb;
    }

    public static LineSegment fromByteBuffer(final ByteBuffer bb) {
        final double x1 = bb.getDouble();
        final double y1 = bb.getDouble();
        final double x2 = bb.getDouble();
        final double y2 = bb.getDouble();
        return LineSegment.of(x1, y1, x2, y2);
    }

    public static double getDouble (final Map<?,?> map, final Keyword key) {
        return NumTool.toDouble(map.get(key));
    }

    public static LineSegment fromMap(final Map<?,?> map) {
        final double x1 = getDouble(map, KW.x1);
        final double y1 = getDouble(map, KW.y1);
        final double x2 = getDouble(map, KW.x2);
        final double y2 = getDouble(map, KW.y2);
        return LineSegment.of(x1, y1, x2, y2);
    }

    public static LineSegment fromList(final List<?> list) {
        final Object p1obj = list.get(0);
        final Object p2obj = list.get(1);
        final Point p1 = Point.fromObject(p1obj);
        final Point p2 = Point.fromObject(p2obj);
        return LineSegment.of(p1, p2);
    }

    public static LineSegment fromString(final String text) {
        final List<String> parts = StrTool.split(text, "[\\[(,)\\]]");
        if (parts.size() != 4) {
            throw new PGError("wrong line segment string: %s", text);
        }
        final double x1 = Double.parseDouble(parts.get(0));
        final double y1 = Double.parseDouble(parts.get(1));
        final double x2 = Double.parseDouble(parts.get(2));
        final double y2 = Double.parseDouble(parts.get(3));
        return LineSegment.of(x1, y1, x2, y2);
    }

    @SuppressWarnings("unused")
    public static LineSegment fromObject(final Object x) {
        if (x instanceof Map<?,?> m) {
            return LineSegment.fromMap(m);
        } else if (x instanceof List<?> l) {
            return LineSegment.fromList(l);
        } else if (x instanceof ByteBuffer bb) {
            return LineSegment.fromByteBuffer(bb);
        } else if (x instanceof String s) {
            return LineSegment.fromString(s);
        } else if (x instanceof LineSegment ls) {
            return ls;
        } else {
            throw PGError.error("wrong line segment input: %s", x);
        }
    }

    public Object toClojure() {
        return PersistentVector.create(
                p1.toClojure(),
                p2.toClojure()
        );
    }

    public String toSQL() {
        return "[" + p1.toSQL() + "," + p2.toSQL() + "]";
    }
}
