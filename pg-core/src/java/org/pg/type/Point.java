package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;
import org.pg.error.PGError;
import org.pg.util.NumTool;
import org.pg.util.StrTool;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public record Point (double x, double y) {

    public static Point of(final double x, final double y) {
        return new Point(x, y);
    }

    public IPersistentMap toClojure() {
        return PersistentHashMap.create(KW.x, x, KW.y, y);
    }

    public ByteBuffer toByteBuffer() {
        final ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putDouble(x);
        bb.putDouble(y);
        return bb;
    }

    public static Point fromByteBuffer(final ByteBuffer bb) {
        final double x = bb.getDouble();
        final double y = bb.getDouble();
        return Point.of(x, y);
    }

    public static Point fromMap(final Map<?,?> map) {
        final double x = NumTool.toDouble(map.get(KW.x));
        final double y = NumTool.toDouble(map.get(KW.y));
        return Point.of(x, y);
    }

    public static Point fromObject(final Object x) {
        if (x instanceof Map<?,?> m) {
            return Point.fromMap(m);
        } else if (x instanceof List<?> l) {
            return Point.fromList(l);
        } else if (x instanceof ByteBuffer bb) {
            return Point.fromByteBuffer(bb);
        } else if (x instanceof String s) {
            return Point.fromSQL(s);
        } else if (x instanceof Point p) {
            return p;
        } else {
            throw PGError.error("wrong point input: %s", x);
        }
    }

    public static Point fromList(final List<?> list) {
        final double x = NumTool.toDouble(list.get(0));
        final double y = NumTool.toDouble(list.get(1));
        return Point.of(x, y);
    }

    public static Point fromSQL(final String text) {
        final List<String> parts = StrTool.split(text, "[(,)]");
        if (parts.size() != 2) {
            throw new PGError("wrong point string: %s", text);
        }
        final double x = Double.parseDouble(parts.get(0));
        final double y = Double.parseDouble(parts.get(1));
        return Point.of(x, y);
    }

    public String toSQL() {
        return "(" + x + "," + y + ")";
    }

    public static void main(String... args) {
        System.out.println(Point.fromSQL("(1.23342 , -3.23234)"));
    }
}
