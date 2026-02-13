package org.pg.type;

import clojure.lang.IDeref;
import clojure.lang.PersistentHashMap;
import org.pg.clojure.KW;
import org.pg.clojure.RowMap;

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

    public static String buildFullName(final String schema, final String type) {
        return schema + '.' + type;
    }

    /*
    Return a full qualified name of the type, e.g. schema.type_name.
     */
    public String fullName() {
        return buildFullName(nspname, typname);
    }

    public static PGType fromRowMap(final RowMap row) {
        return new PGType(
                (int)    row.nth(0),
                (String) row.nth(1),
                (char)   row.nth(2),
                (String) row.nth(3),
                (String) row.nth(4),
                (String) row.nth(5),
                (String) row.nth(6),
                (int)    row.nth(7),
                (char)   row.nth(8),
                (int)    row.nth(9),
                (String) row.nth(10)
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
