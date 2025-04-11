package org.pg.type;

import clojure.lang.IDeref;
import clojure.lang.PersistentHashMap;
import org.pg.clojure.KW;
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
) implements IDeref {

    public boolean isEnum() {
        return typtype == 'e';
    }

    public boolean isArray() {
        return typelem != 0;
    }

    public boolean isElem() {
        return typarray != 0;
    }

    /*
    A unique string specifying the current Postgres type.
    Serves as a key to connect a pgType with a custom processor.
     */
    public String signature() {
        return String.format("%s/%s", typname, typinput);
    }

    /*
    Return a full qualified name of the type, e.g. schema.type_name.
     */
    public String fullName() {
        return nspname() + "." + typname();
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

    @Override
    public Object deref() {
        return PersistentHashMap.create(
                KW.oid, oid,
                KW.typname, typname,
                KW.typtype, typtype,
                KW.typinput, typinput,
                KW.typoutput, typoutput,
                KW.typreceive, typreceive,
                KW.typsend, typsend,
                KW.typarray, typarray,
                KW.typdelim, typdelim,
                KW.typelem, typelem,
                KW.nspname, nspname
        );
    }
}
