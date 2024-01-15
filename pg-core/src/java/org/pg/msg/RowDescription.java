package org.pg.msg;

import org.pg.util.BBTool;
import org.pg.enums.Format;
import org.pg.enums.OID;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record RowDescription (
        short columnCount,
        Column[] columns
) {

    public record Column (
            int index,
            String name,
            int tableOid,
            int columnOid,
            OID typeOid,
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

    public static RowDescription fromByteBuffer(final ByteBuffer buf, Charset charset) {
        final short size = buf.getShort();
        final Column[] columns = new Column[size];
        for (short i = 0; i < size; i++) {
            final Column col = new Column(i,
                    BBTool.getCString(buf, charset),
                    buf.getInt(),
                    buf.getShort(),
                    OID.ofInt(buf.getInt(), OID.TEXT),
                    buf.getShort(),
                    buf.getInt(),
                    Format.ofShort(buf.getShort()));
            columns[i] = col;
        }
        return new RowDescription(size, columns);
    }
}
