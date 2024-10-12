package org.pg.type.processor;

import org.pg.codec.CodecParams;
import org.pg.enums.OID;
import org.pg.error.PGError;
import org.pg.util.BBTool;

import java.nio.ByteBuffer;

public class Bool extends AProcessor {

    public static final int oid = OID.BOOL;

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Boolean b) {
            return BBTool.ofBool(b);
        } else {
            return binEncodingError(x, oid);
        }
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        if (x instanceof Boolean b) {
            return b ? "t" : "f";
        } else {
            return txtEncodingError(x, oid);
        }
    }

    @Override
    public Boolean decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        final byte b = bb.get();
        return switch (b) {
            case 0: yield false;
            case 1: yield true;
            default: throw new PGError("incorrect binary boolean value: %s", b);
        };
    }

    @Override
    public Boolean decodeTxt(final String string, final CodecParams codecParams) {
        return switch (string) {
            case "t" -> true;
            case "f" -> false;
            default -> throw new PGError("wrong boolean value: %s", string);
        };
    }
}
