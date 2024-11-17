package org.pg.type;

import clojure.lang.*;
import org.pg.clojure.KW;
import org.pg.error.PGError;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public record Box(double x1, double y1, double x2, double y2)
    implements IDeref {

    public ByteBuffer toByteBuffer() {
        final ByteBuffer bb = ByteBuffer.allocate(32);
        bb.putDouble(x1);
        bb.putDouble(y1);
        bb.putDouble(x2);
        bb.putDouble(y2);
        return bb;
    }

    public static org.pg.type.Box fromByteBuffer(final ByteBuffer bb) {
        final double x1 = bb.getDouble();
        final double y1 = bb.getDouble();
        final double x2 = bb.getDouble();
        final double y2 = bb.getDouble();
        return new Box(x1,y1, x2, y2);
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
        return new Box(x1, y1, x2, y2);
    }

    @Override
    public String toString() {
        return "(" + x1 + "," + y1 + "),(" + x2 + "," + y2 + ")";
    }

    @Override
    public Object deref() {
        return PersistentHashMap.create(
                KW.x1, x1,
                KW.y1, y1,
                KW.x2, x2,
                KW.y2, y2
        );
    }
}
