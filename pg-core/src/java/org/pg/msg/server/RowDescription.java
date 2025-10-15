package org.pg.msg.server;

import org.pg.enums.Format;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public record RowDescription (
        short columnCount,
        Column[] columns
) implements IServerMessage {

    @Override
    public String toString() {
        return String.format("RowDescription[columnCount=%s, columns=%s]",
                columnCount,
                Arrays.toString(columns)
        );
    }

    public int[] oids () {
        final int[] oids = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            oids[i] = columns[i].typeOid;
        }
        return oids;
    }

    public record Column (
            int index,
            String name,
            int tableOid,
            int columnOid,
            int typeOid,
            short typeLen,
            int typeMod,
            Format format) {
    }

    public String [] getColumnNames () {
        final String[] names = new String[columnCount];
        for (short i = 0; i < columnCount; i++) {
            names[i] = columns()[i].name();
        }
        return names;
    }

    public static RowDescription fromByteBuffer(final ByteBuffer bb, Charset charset) {
        final short size = bb.getShort();
        final Column[] columns = new Column[size];
        for (short i = 0; i < size; i++) {
            final Column col = new Column(i,
                    BBTool.getCString(bb, charset),
                    bb.getInt(),
                    bb.getShort(),
                    bb.getInt(),
                    bb.getShort(),
                    bb.getInt(),
                    Format.ofShort(bb.getShort()));
            columns[i] = col;
        }
        return new RowDescription(size, columns);
    }
}
