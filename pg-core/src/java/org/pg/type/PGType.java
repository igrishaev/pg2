package org.pg.type;

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

}
