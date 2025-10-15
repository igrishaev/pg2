package org.pg.msg.server;

import org.pg.util.ArrayTool;
import org.pg.enums.Format;

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

    public static RowDescription fromBytes(final byte[] bytes, Charset charset) {
        final int[] off = {0};
        final short size = ArrayTool.readShort(bytes, off);
        final Column[] columns = new Column[size];
        for (short i = 0; i < size; i++) {
            final Column col = new Column(i,
                    ArrayTool.readCString(bytes, off, charset),
                    ArrayTool.readInt(bytes, off),
                    ArrayTool.readShort(bytes, off),
                    ArrayTool.readInt(bytes, off),
                    ArrayTool.readShort(bytes, off),
                    ArrayTool.readInt(bytes, off),
                    Format.ofShort(ArrayTool.readShort(bytes, off)));
            columns[i] = col;
        }
        return new RowDescription(size, columns);
    }
}
