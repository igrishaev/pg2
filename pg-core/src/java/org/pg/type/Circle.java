package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;
import org.pg.error.PGError;
import org.pg.util.NumTool;
import org.pg.util.StrTool;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public record Circle(double x, double y, double r) {

    public static Circle of(double x, double y, double r) {
        return new Circle(x, y ,r);
    }

    public ByteBuffer toByteBuffer() {
        final ByteBuffer bb = ByteBuffer.allocate(24);
        bb.putDouble(x);
        bb.putDouble(y);
        bb.putDouble(r);
        return bb;
    }

    public static Circle fromByteBuffer(final ByteBuffer bb) {
        final double x = bb.getDouble();
        final double y = bb.getDouble();
        final double r = bb.getDouble();
        return Circle.of(x, y, r);
    }

    public static Circle fromString(final String text) {
        final List<String> parts = StrTool.split(text, "[<(,)>]");
        if (parts.size() != 3) {
            throw new PGError("wrong circle string: %s", text);
        }
        final double x = Double.parseDouble(parts.get(0));
        final double y = Double.parseDouble(parts.get(1));
        final double r = Double.parseDouble(parts.get(2));
        return Circle.of(x, y, r);
    }

    public static Circle fromList(final List<?> list) {
        final double x = NumTool.toDouble(list.get(0));
        final double y = NumTool.toDouble(list.get(1));
        final double r = NumTool.toDouble(list.get(2));
        return Circle.of(x, y, r);
    }

    public static Circle fromMap(final Map<?,?> map) {
        final double x = NumTool.toDouble(map.get(KW.x));
        final double y = NumTool.toDouble(map.get(KW.y));
        final double r = NumTool.toDouble(map.get(KW.r));
        return Circle.of(x, y, r);
    }

    @SuppressWarnings("unused")
    public static Circle fromObject(final Object x) {
        if (x instanceof Map<?,?> m) {
            return Circle.fromMap(m);
        } else if (x instanceof List<?> l) {
            return Circle.fromList(l);
        } else if (x instanceof ByteBuffer bb) {
            return Circle.fromByteBuffer(bb);
        } else if (x instanceof String s) {
            return Circle.fromString(s);
        } else if (x instanceof Circle c) {
            return c;
        } else {
            throw PGError.error("wrong point input: %s", x);
        }
    }

    public String toSQL() {
        return "<(" + x + "," + y + ")," + r + ">";
    }

    public Object toClojure() {
        return PersistentHashMap.create(
                KW.x, x,
                KW.y, y,
                KW.r, r
        );
    }
}
