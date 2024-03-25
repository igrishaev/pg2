
package org.pg.codec;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.pg.error.PGError;
import org.pg.enums.OID;
import org.pg.util.BBTool;
import org.pg.json.JSON;


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
            case TEXT, VARCHAR, NAME -> BBTool.getRestString(buf, codecParams.serverCharset());
            case INT2 -> buf.getShort();
            case INT4, OID -> buf.getInt();
            case INT8 -> buf.getLong();
            case CHAR, BPCHAR -> BBTool.getRestString(buf, codecParams.serverCharset()).charAt(0);
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
            case JSON -> JSON.readValue(codecParams.objectMapper(), buf);
            case JSONB -> {
                buf.get(); // skip version
                yield JSON.readValue(codecParams.objectMapper(), buf);
            }
            case TIME -> DateTimeBin.decodeTIME(buf);
            case TIMETZ -> DateTimeBin.decodeTIMETZ(buf);
            case DATE -> DateTimeBin.decodeDATE(buf);
            case TIMESTAMP -> DateTimeBin.decodeTIMESTAMP(buf);
            case TIMESTAMPTZ -> DateTimeBin.decodeTIMESTAMPTZ(buf);
            case NUMERIC -> NumericBin.decode(buf);
            case _TEXT, _VARCHAR, _NAME, _INT2, _INT4, _INT8, _OID, _CHAR, _BPCHAR, _UUID,
                    _FLOAT4, _FLOAT8, _BOOL, _JSON, _JSONB, _TIME, _TIMETZ, _DATE, _TIMESTAMP,
                    _TIMESTAMPTZ, _NUMERIC -> ArrayBin.decode(buf, oid, codecParams);
            default -> BBTool.getRestBytes(buf);
        };
    }

}