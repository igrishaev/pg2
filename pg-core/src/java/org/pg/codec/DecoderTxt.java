package org.pg.codec;

import org.pg.Const;
import org.pg.error.PGError;
import org.pg.enums.OID;
import org.pg.util.HexTool;
import org.pg.json.JSON;

import java.util.Arrays;

import java.util.UUID;
import java.math.BigDecimal;

public final class DecoderTxt {

    public static Object decode(
            final String string,
            final int oid
    ) {
        return decode(string, oid, CodecParams.standard());
    }

    public static Object decode(
            final String string,
            final int oid,
            final CodecParams codecParams
    ) {

        return switch (oid) {

            case OID.INT2 -> Short.parseShort(string);
            case OID.INT4, OID.OID -> Integer.parseInt(string);
            case OID.INT8 -> Long.parseLong(string);
            case OID.BYTEA -> HexTool.parseHex(string, 2, string.length());
            case OID.CHAR -> {
                if (string.isEmpty()) {
                    yield Const.NULL_CHAR;
                } else {
                    yield string.charAt(0);
                }
            }
            case OID.UUID -> UUID.fromString(string);
            case OID.FLOAT4 -> Float.parseFloat(string);
            case OID.FLOAT8 -> Double.parseDouble(string);
            case OID.NUMERIC -> new BigDecimal(string);
            case OID.BOOL -> switch (string) {
                    case "t" -> true;
                    case "f" -> false;
                    default -> throw new PGError("wrong boolean value: %s", string);
            };
            case OID.JSON, OID.JSONB -> JSON.readValue(codecParams.objectMapper(), string);
            case OID.TIMESTAMPTZ -> DateTimeTxt.decodeTIMESTAMPTZ(string);
            case OID.TIMESTAMP -> DateTimeTxt.decodeTIMESTAMP(string);
            case OID.DATE -> DateTimeTxt.decodeDATE(string);
            case OID.TIMETZ -> DateTimeTxt.decodeTIMETZ(string);
            case OID.TIME -> DateTimeTxt.decodeTIME(string);
//            case OID._TEXT, OID._VARCHAR, OID._NAME, OID._INT2, OID._INT4, OID._INT8, OID._OID, OID._CHAR, OID._BPCHAR, OID._UUID,
//                    OID._FLOAT4, OID._FLOAT8, OID._BOOL, OID._JSON, OID._JSONB, OID._TIME, OID._TIMETZ, OID._DATE, OID._TIMESTAMP,
//                    OID._TIMESTAMPTZ, OID._NUMERIC -> ArrayTxt.decode(string, oid, codecParams);
            default -> string;
        };
    }

    public static void main (final String[] args) {
        final String string = "\\xDEADBEEF";
        System.out.println(Arrays.toString(HexTool.parseHex(string, 2, string.length())));
    }

}
