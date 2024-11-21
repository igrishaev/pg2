package org.pg.util;

import org.pg.error.PGError;
import org.pg.type.Point;

import java.util.ArrayList;
import java.util.List;

public class GeomTool {

    public static List<Point> parsePoints(Iterable<?> rawPoints) {
        final List<Point> points = new ArrayList<>();
        Point point;
        for (Object x: rawPoints) {
            point = Point.fromObject(x);
            points.add(point);
        }
        return points;
    }

    public static List<Point> parsePoints(final List<String> parts) {
        final int len = parts.size();
        if (len % 2 != 0) {
            throw new PGError("number of values must be even: %s", parts.size());
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
        return points;
    }


}
