
package org.pg.codec;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.pg.PGError;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.type.JSON;


public final class DecoderBin {

    public static Object decode(final ByteBuffer buf, final OID oid) {
        return decode(buf, oid, CodecParams.standard());
    }

    public static Object decode(
            final ByteBuffer buf,
            final OID oid,
            final CodecParams codecParams
    ) {
        return switch (oid) {
            case TEXT, VARCHAR, NAME -> BBTool.getRestString(buf, codecParams.serverCharset);
            case INT2 -> buf.getShort();
            case INT4, OID -> buf.getInt();
            case INT8 -> buf.getLong();
            case CHAR, BPCHAR -> BBTool.getRestString(buf, codecParams.serverCharset).charAt(0);
            case UUID -> {
                final long hiBits = buf.getLong();
                final long loBits = buf.getLong();
                yield new UUID(hiBits, loBits);
            }
            case FLOAT4 -> buf.getFloat();
            case FLOAT8 -> buf.getDouble();
            case BOOL -> {
                switch (buf.get()) {
                    case 0: yield false;
                    case 1: yield true;
                    default: throw new PGError("incorrect binary boolean value");
                }
            }
            case JSON, JSONB -> JSON.readValueBinary(buf);
            case TIME -> DateTimeBin.decodeTIME(buf);
            case TIMETZ -> DateTimeBin.decodeTIMETZ(buf);
            case DATE -> DateTimeBin.decodeDATE(buf);
            case TIMESTAMP -> DateTimeBin.decodeTIMESTAMP(buf);
            case TIMESTAMPTZ -> DateTimeBin.decodeTIMESTAMPTZ(buf);
            case NUMERIC -> NumericBin.decode(buf);
            default -> BBTool.getRestBytes(buf);
        };
    }

}