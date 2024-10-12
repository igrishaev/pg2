
package org.pg.codec;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.pg.Const;
import org.pg.error.PGError;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.json.JSON;

public final class DecoderBin {

    public static Object decode(final ByteBuffer buf, final int oid) {
        return decode(buf, oid, CodecParams.standard());
    }

    public static Object decode(
            final ByteBuffer buf,
            final int oid,
            final CodecParams codecParams
    ) {
        return switch (oid) {
            case OID.TEXT, OID.VARCHAR, OID.NAME, OID.BPCHAR -> BBTool.getRestString(buf, codecParams.serverCharset());
            case OID.INT2 -> buf.getShort();
            case OID.INT4, OID.OID -> buf.getInt();
            case OID.INT8 -> buf.getLong();
            case OID.CHAR -> {
                final String string = BBTool.getRestString(buf, codecParams.serverCharset());
                if (string.isEmpty()) {
                    yield Const.NULL_CHAR;
                } else {
                    yield string.charAt(0);
                }
            }
            case OID.UUID -> {
                final long hiBits = buf.getLong();
                final long loBits = buf.getLong();
                yield new UUID(hiBits, loBits);
            }
            case OID.FLOAT4 -> buf.getFloat();
            case OID.FLOAT8 -> buf.getDouble();
            case OID.BOOL -> {
                switch (buf.get()) {
                    case 0: yield false;
                    case 1: yield true;
                    default: throw new PGError("incorrect binary boolean value");
                }
            }
            case OID.JSON -> JSON.readValue(codecParams.objectMapper(), buf);
            case OID.JSONB -> {
                buf.get(); // skip version
                yield JSON.readValue(codecParams.objectMapper(), buf);
            }
            case OID.TIME -> DateTimeBin.decodeTIME(buf);
            case OID.TIMETZ -> DateTimeBin.decodeTIMETZ(buf);
            case OID.DATE -> DateTimeBin.decodeDATE(buf);
            case OID.TIMESTAMP -> DateTimeBin.decodeTIMESTAMP(buf);
            case OID.TIMESTAMPTZ -> DateTimeBin.decodeTIMESTAMPTZ(buf);
            case OID.NUMERIC -> NumericBin.decode(buf);
//            case OID._TEXT, OID._VARCHAR, OID._NAME, OID._INT2, OID._INT4, OID._INT8, OID._OID, OID._CHAR, OID._BPCHAR, OID._UUID,
//                    OID._FLOAT4, OID._FLOAT8, OID._BOOL, OID._JSON, OID._JSONB, OID._TIME, OID._TIMETZ, OID._DATE, OID._TIMESTAMP,
//                    OID._TIMESTAMPTZ, OID._NUMERIC -> ArrayBin.decode(buf, oid, codecParams);
            default -> BBTool.getRestBytes(buf);
        };
    }

}