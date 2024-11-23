package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;
import org.pg.error.PGError;
import org.pg.util.CljTool;
import org.pg.util.NumTool;
import org.pg.util.StrTool;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public record Point (double x, double y) implements IPersistentMap, IDeref {

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

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    public static void main(String... args) {
        System.out.println(Point.fromSQL("(1.23342 , -3.23234)"));
        System.out.println(Point.of(1.0, 2.0).equals(Point.of(1.0, 2.0)));
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof Keyword kw) {
            return kw.equals(KW.x) || kw.equals(KW.y);
        }
        return false;
    }

    @Override
    public IMapEntry entryAt(Object key) {
        if (key instanceof Keyword k) {
            if (k == KW.x) {
                return CljTool.mapEntry(key, x);
            } else if (k == KW.y) {
                return CljTool.mapEntry(key, y);
            }
        }
        return null;
    }

    @Override
    public IPersistentMap assoc(Object key, Object val) {
        return toClojure().assoc(key, val);
    }

    @Override
    public IPersistentMap assocEx(Object key, Object val) {
        return toClojure().assocEx(key, val);
    }

    @Override
    public IPersistentMap without(Object key) {
        return toClojure().without(key);
    }

    @Override
    public Object valAt(Object key) {
        return valAt(key, null);
    }

    @Override
    public Object valAt(Object key, Object notFound) {
        if (key instanceof Keyword k) {
            if (k == KW.x) {
                return x;
            } else if (k == KW.y) {
                return y;
            }
        }
        return notFound;
    }

    @Override
    public int count() {
        return 2;
    }

    @Override
    public IPersistentCollection cons(Object o) {
        return toClojure().cons(o);
    }

    @Override
    public IPersistentCollection empty() {
        return PersistentHashMap.EMPTY;
    }

    @Override
    public boolean equiv(Object o) {
        if (o instanceof Point p) {
            return this.equals(p);
        }
        return toClojure().equiv(o);
    }

    @Override
    public ISeq seq() {
        return toClojure().seq();
    }

    @Override
    public Iterator<?> iterator() {
        return toClojure().iterator();
    }

    @Override
    public Object deref() {
        return toClojure();
    }
}
