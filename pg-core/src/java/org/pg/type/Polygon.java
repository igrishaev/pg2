package org.pg.type;

import clojure.lang.Counted;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import clojure.java.api.Clojure;
import org.pg.error.PGError;
import org.pg.util.StrTool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public record Polygon (List<Point> points) implements Counted, IDeref, Iterable<Point> {

    public static IFn mapv = Clojure.var("clojure.core", "mapv");
    public static IFn deref = Clojure.var("clojure.core", "deref");

    public static Polygon of(List<Point> points) {
        return new Polygon(points);
    }

    public static Polygon fromList(Iterable<?> rawPoints) {
        final List<Point> points = new ArrayList<>();
        Point point;
        for (Object x: rawPoints) {
            point = Point.fromObject(x);
            points.add(point);
        }
        return new Polygon(points);
    }

    public ByteBuffer toByteBuffer() {
        final int len = points.size();
        final ByteBuffer bb = ByteBuffer.allocate(4 + len * 16);
        bb.putInt(len);
        for (Point p: points) {
            bb.putDouble(p.x());
            bb.putDouble(p.y());
        }
        return bb;
    }

    public static Polygon fromByteBuffer(final ByteBuffer bb) {
        final int len = bb.getInt();
        final List<Point> points = new ArrayList<>(len);
        Point point;
        for (int i = 0; i < len; i++) {
            point = Point.fromByteBuffer(bb);
            points.add(point);
        }
        return Polygon.of(points);
    }

    public static Polygon fromString(final String text) {
        final List<String> parts = StrTool.split(text, "[(,)]");
        final int len = parts.size();
        if (len % 2 != 0) {
            throw new PGError("number of values must be even: %s, %s", parts.size(), text);
        }
        int i = 0;
        double x, y;
        final List<Point> points = new ArrayList<>(len / 2);
        Point point;
        while (i < len) {
            x = Double.parseDouble(parts.get(i));
            i++;
            y = Double.parseDouble(parts.get(i));
            i++;
            point = Point.of(x, y);
            points.add(point);
        }
        return Polygon.of(points);
    }

    @SuppressWarnings("unused")
    public static Polygon fromObject(final Object x) {
        if (x instanceof List<?> l) {
            return Polygon.fromList(l);
        } else if (x instanceof ByteBuffer bb) {
            return Polygon.fromByteBuffer(bb);
        } else if (x instanceof String s) {
            return Polygon.fromString(s);
        } else if (x instanceof Polygon p) {
            return p;
        } else {
            throw PGError.error("wrong polygon input: %s", x);
        }
    }

    @Override
    public String toString() {
        return String.join(",", points.stream().map(Point::toString).toList());
    }

    @Override
    public int count() {
        return points.size();
    }

    @Override
    public Object deref() {
        return mapv.invoke(deref, points);
    }

    @Override
    public Iterator<Point> iterator() {
        return points.iterator();
    }
}
