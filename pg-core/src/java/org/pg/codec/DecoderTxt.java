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
            final OID oid
    ) {
        return decode(string, oid, CodecParams.standard());
    }

    public static Object decode(
            final String string,
            final OID oid,
            final CodecParams codecParams
    ) {

        return switch (oid) {

            case INT2 -> Short.parseShort(string);
            case INT4, OID -> Integer.parseInt(string);
            case INT8 -> Long.parseLong(string);
            case BYTEA -> HexTool.parseHex(string, 2, string.length());
            case CHAR -> {
                if (string.isEmpty()) {
                    yield Const.NULL_CHAR;
                } else {
                    yield string.charAt(0);
                }
            }
            case UUID -> UUID.fromString(string);
            case FLOAT4 -> Float.parseFloat(string);
            case FLOAT8 -> Double.parseDouble(string);
            case NUMERIC -> new BigDecimal(string);
            case BOOL -> switch (string) {
                    case "t" -> true;
                    case "f" -> false;
                    default -> throw new PGError("wrong boolean value: %s", string);
            };
            case JSON, JSONB -> JSON.readValue(codecParams.objectMapper(), string);
            case TIMESTAMPTZ -> DateTimeTxt.decodeTIMESTAMPTZ(string);
            case TIMESTAMP -> DateTimeTxt.decodeTIMESTAMP(string);
            case DATE -> DateTimeTxt.decodeDATE(string);
            case TIMETZ -> DateTimeTxt.decodeTIMETZ(string);
            case TIME -> DateTimeTxt.decodeTIME(string);
            case _TEXT, _VARCHAR, _NAME, _INT2, _INT4, _INT8, _OID, _CHAR, _BPCHAR, _UUID,
                    _FLOAT4, _FLOAT8, _BOOL, _JSON, _JSONB, _TIME, _TIMETZ, _DATE, _TIMESTAMP,
                    _TIMESTAMPTZ, _NUMERIC -> ArrayTxt.decode(string, oid, codecParams);
            default -> string;
        };
    }

    public static void main (final String[] args) {
        final String string = "\\xDEADBEEF";
        System.out.println(Arrays.toString(HexTool.parseHex(string, 2, string.length())));
    }

}
