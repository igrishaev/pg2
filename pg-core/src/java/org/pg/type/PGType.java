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

    /*
    Parse a 'COPY TO' binary row manually and compose an instance of PGType.
     */
    public static PGType fromCopyBuffer(final ByteBuffer bb) {
        int len;

        BBTool.skip(bb, 2);

        BBTool.skip(bb, 4);
        final int oid = bb.getInt();

        len = bb.getInt();
        final String typname = BBTool.getString(bb, len);

        BBTool.skip(bb, 4);
        final char typtype = (char) bb.get();

        len = bb.getInt();
        final String typinput = BBTool.getString(bb, len);

        len = bb.getInt();
        final String typoutput = BBTool.getString(bb, len);

        len = bb.getInt();
        final String typreceive = BBTool.getString(bb, len);

        len = bb.getInt();
        final String typsend = BBTool.getString(bb, len);

        BBTool.skip(bb, 4);
        final int typarray = bb.getInt();

        BBTool.skip(bb, 4);
        final char typdelim = (char) bb.get();

        BBTool.skip(bb, 4);
        final int typelem = bb.getInt();

        len = bb.getInt();
        final String nspname = BBTool.getString(bb, len);

        return new PGType(
                oid,
                typname,
                typtype,
                typinput,
                typoutput,
                typreceive,
                typsend,
                typarray,
                typdelim,
                typelem,
                nspname
        );
    }
}
