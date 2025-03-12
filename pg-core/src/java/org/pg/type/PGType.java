package org.pg.type;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public record PGType(
        int oid,
        String typname,
        char typtype,
        String typinput,
        String typoutput,
        String typreceive,
        String typsend,
        int typarray,
        char typdelim,
        int typelem,
        String nspname
) {

    public boolean isEnum() {
        return typtype == 'e';
    }

    public boolean isVector() {
        return typname.equals("vector") && typinput.equals("vector_in");
    }

    public boolean isSparseVector() {
        return typname.equals("sparsevec") && typinput.equals("sparsevec_in");
    }

    public static PGType fromByteBuffer(final ByteBuffer bb) {
        int len = 0;

        BBTool.skip(bb, 2);

        // oid
        len = bb.getInt();
        final int oid = Integer.parseInt(BBTool.getString(bb, len));

        len = bb.getInt();
        final String typname = BBTool.getString(bb, len);



        return null;
    }

    // TODO: from RowMap
//    final PGType pgType = new PGType(
//            oid, --
//            (String) rowMap.get("typname"), --
//            (char) rowMap.get("typtype"),
//            (String) rowMap.get("typinput"),
//            (String) rowMap.get("typoutput"),
//            (String) rowMap.get("typreceive"),
//            (String) rowMap.get("typsend"),
//            (int) rowMap.get("typarray"),
//            (char) rowMap.get("typdelim"),
//            (int) rowMap.get("typelem"),
//            (String) rowMap.get("nspname")
//    );

}
