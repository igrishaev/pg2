package org.pg.type;

import clojure.lang.Counted;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import clojure.java.api.Clojure;

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

    public static Polygon of(Iterable<?> rawPoints) {
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
