package org.pg.enums;

import clojure.lang.BigInt;
import clojure.lang.IPersistentMap;
import clojure.lang.Symbol;
import org.pg.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.Date;

@SuppressWarnings("unused")
public class OID {
    public static final int DEFAULT                       =    0;
    public static final int BOOL                          =   16;
    public static final int _BOOL                         = 1000;
    public static final int BYTEA                         =   17;
    public static final int _BYTEA                        = 1001;
    public static final int CHAR                          =   18;
    public static final int _CHAR                         = 1002;
    public static final int NAME                          =   19;
    public static final int _NAME                         = 1003;
    public static final int INT8                          =   20;
    public static final int _INT8                         = 1016;
    public static final int INT2                          =   21;
    public static final int _INT2                         = 1005;
    public static final int INT2VECTOR                    =   22;
    public static final int _INT2VECTOR                   = 1006;
    public static final int INT4                          =   23;
    public static final int _INT4                         = 1007;
    public static final int REGPROC                       =   24;
    public static final int _REGPROC                      = 1008;
    public static final int TEXT                          =   25;
    public static final int _TEXT                         = 1009;
    public static final int OID                           =   26;
    public static final int _OID                          = 1028;
    public static final int TID                           =   27;
    public static final int _TID                          = 1010;
    public static final int XID                           =   28;
    public static final int _XID                          = 1011;
    public static final int CID                           =   29;
    public static final int _CID                          = 1012;
    public static final int OIDVECTOR                     =   30;
    public static final int _OIDVECTOR                    = 1013;
    public static final int PG_TYPE                       =   71;
    public static final int _PG_TYPE                      =  210;
    public static final int PG_ATTRIBUTE                  =   75;
    public static final int _PG_ATTRIBUTE                 =  270;
    public static final int PG_PROC                       =   81;
    public static final int _PG_PROC                      =  272;
    public static final int PG_CLASS                      =   83;
    public static final int _PG_CLASS                     =  273;
    public static final int JSON                          =  114;
    public static final int _JSON                         =  199;
    public static final int XML                           =  142;
    public static final int _XML                          =  143;
    public static final int PG_NODE_TREE                  =  194;
    public static final int PG_NDISTINCT                  = 3361;
    public static final int PG_DEPENDENCIES               = 3402;
    public static final int PG_MCV_LIST                   = 5017;
    public static final int PG_DDL_COMMAND                =   32;
    public static final int XID8                          = 5069;
    public static final int _XID8                         =  271;
    public static final int POINT                         =  600;
    public static final int _POINT                        = 1017;
    public static final int LSEG                          =  601;
    public static final int _LSEG                         = 1018;
    public static final int PATH                          =  602;
    public static final int _PATH                         = 1019;
    public static final int BOX                           =  603;
    public static final int _BOX                          = 1020;
    public static final int POLYGON                       =  604;
    public static final int _POLYGON                      = 1027;
    public static final int LINE                          =  628;
    public static final int _LINE                         =  629;
    public static final int FLOAT4                        =  700;
    public static final int _FLOAT4                       = 1021;
    public static final int FLOAT8                        =  701;
    public static final int _FLOAT8                       = 1022;
    public static final int UNKNOWN                       =  705;
    public static final int CIRCLE                        =  718;
    public static final int _CIRCLE                       =  719;
    public static final int MONEY                         =  790;
    public static final int _MONEY                        =  791;
    public static final int MACADDR                       =  829;
    public static final int _MACADDR                      = 1040;
    public static final int INET                          =  869;
    public static final int _INET                         = 1041;
    public static final int CIDR                          =  650;
    public static final int _CIDR                         =  651;
    public static final int MACADDR8                      =  774;
    public static final int _MACADDR8                     =  775;
    public static final int ACLITEM                       = 1033;
    public static final int _ACLITEM                      = 1034;
    public static final int BPCHAR                        = 1042;
    public static final int _BPCHAR                       = 1014;
    public static final int VARCHAR                       = 1043;
    public static final int _VARCHAR                      = 1015;
    public static final int DATE                          = 1082;
    public static final int _DATE                         = 1182;
    public static final int TIME                          = 1083;
    public static final int _TIME                         = 1183;
    public static final int TIMESTAMP                     = 1114;
    public static final int _TIMESTAMP                    = 1115;
    public static final int TIMESTAMPTZ                   = 1184;
    public static final int _TIMESTAMPTZ                  = 1185;
    public static final int INTERVAL                      = 1186;
    public static final int _INTERVAL                     = 1187;
    public static final int TIMETZ                        = 1266;
    public static final int _TIMETZ                       = 1270;
    public static final int BIT                           = 1560;
    public static final int _BIT                          = 1561;
    public static final int VARBIT                        = 1562;
    public static final int _VARBIT                       = 1563;
    public static final int NUMERIC                       = 1700;
    public static final int _NUMERIC                      = 1231;
    public static final int REFCURSOR                     = 1790;
    public static final int _REFCURSOR                    = 2201;
    public static final int REGPROCEDURE                  = 2202;
    public static final int _REGPROCEDURE                 = 2207;
    public static final int REGOPER                       = 2203;
    public static final int _REGOPER                      = 2208;
    public static final int REGOPERATOR                   = 2204;
    public static final int _REGOPERATOR                  = 2209;
    public static final int REGCLASS                      = 2205;
    public static final int _REGCLASS                     = 2210;
    public static final int REGCOLLATION                  = 4191;
    public static final int _REGCOLLATION                 = 4192;
    public static final int REGTYPE                       = 2206;
    public static final int _REGTYPE                      = 2211;
    public static final int REGROLE                       = 4096;
    public static final int _REGROLE                      = 4097;
    public static final int REGNAMESPACE                  = 4089;
    public static final int _REGNAMESPACE                 = 4090;
    public static final int UUID                          = 2950;
    public static final int _UUID                         = 2951;
    public static final int PG_LSN                        = 3220;
    public static final int _PG_LSN                       = 3221;
    public static final int TSVECTOR                      = 3614;
    public static final int _TSVECTOR                     = 3643;
    public static final int GTSVECTOR                     = 3642;
    public static final int _GTSVECTOR                    = 3644;
    public static final int TSQUERY                       = 3615;
    public static final int _TSQUERY                      = 3645;
    public static final int REGCONFIG                     = 3734;
    public static final int _REGCONFIG                    = 3735;
    public static final int REGDICTIONARY                 = 3769;
    public static final int _REGDICTIONARY                = 3770;
    public static final int JSONB                         = 3802;
    public static final int _JSONB                        = 3807;
    public static final int JSONPATH                      = 4072;
    public static final int _JSONPATH                     = 4073;
    public static final int TXID_SNAPSHOT                 = 2970;
    public static final int _TXID_SNAPSHOT                = 2949;
    public static final int PG_SNAPSHOT                   = 5038;
    public static final int _PG_SNAPSHOT                  = 5039;
    public static final int INT4RANGE                     = 3904;
    public static final int _INT4RANGE                    = 3905;
    public static final int NUMRANGE                      = 3906;
    public static final int _NUMRANGE                     = 3907;
    public static final int TSRANGE                       = 3908;
    public static final int _TSRANGE                      = 3909;
    public static final int TSTZRANGE                     = 3910;
    public static final int _TSTZRANGE                    = 3911;
    public static final int DATERANGE                     = 3912;
    public static final int _DATERANGE                    = 3913;
    public static final int INT8RANGE                     = 3926;
    public static final int _INT8RANGE                    = 3927;
    public static final int INT4MULTIRANGE                = 4451;
    public static final int _INT4MULTIRANGE               = 6150;
    public static final int NUMMULTIRANGE                 = 4532;
    public static final int _NUMMULTIRANGE                = 6151;
    public static final int TSMULTIRANGE                  = 4533;
    public static final int _TSMULTIRANGE                 = 6152;
    public static final int TSTZMULTIRANGE                = 4534;
    public static final int _TSTZMULTIRANGE               = 6153;
    public static final int DATEMULTIRANGE                = 4535;
    public static final int _DATEMULTIRANGE               = 6155;
    public static final int INT8MULTIRANGE                = 4536;
    public static final int _INT8MULTIRANGE               = 6157;
    public static final int RECORD                        = 2249;
    public static final int _RECORD                       = 2287;
    public static final int CSTRING                       = 2275;
    public static final int _CSTRING                      = 1263;
    public static final int ANY                           = 2276;
    public static final int ANYARRAY                      = 2277;
    public static final int VOID                          = 2278;
    public static final int TRIGGER                       = 2279;
    public static final int EVENT_TRIGGER                 = 3838;
    public static final int LANGUAGE_HANDLER              = 2280;
    public static final int INTERNAL                      = 2281;
    public static final int ANYELEMENT                    = 2283;
    public static final int ANYNONARRAY                   = 2776;
    public static final int ANYENUM                       = 3500;
    public static final int FDW_HANDLER                   = 3115;
    public static final int INDEX_AM_HANDLER              =  325;
    public static final int TSM_HANDLER                   = 3310;
    public static final int TABLE_AM_HANDLER              =  269;
    public static final int ANYRANGE                      = 3831;
    public static final int ANYCOMPATIBLE                 = 5077;
    public static final int ANYCOMPATIBLEARRAY            = 5078;
    public static final int ANYCOMPATIBLENONARRAY         = 5079;
    public static final int ANYCOMPATIBLERANGE            = 5080;
    public static final int ANYMULTIRANGE                 = 4537;
    public static final int ANYCOMPATIBLEMULTIRANGE       = 4538;
    public static final int PG_BRIN_BLOOM_SUMMARY         = 4600;
    public static final int PG_BRIN_MINMAX_MULTI_SUMMARY  = 4601;

    public static int defaultOID(final Object x) {
        if (x instanceof String s) {
            return TEXT;
        } else if (x instanceof Symbol s) {
            return TEXT;
        } else if (x instanceof Character c) {
            return TEXT;
        } else if (x instanceof Short s) {
            return INT2;
        } else if (x instanceof Integer i) {
            return INT4;
        } else if (x instanceof Long l) {
            return INT8;
        } else if (x instanceof Float f) {
            return FLOAT4;
        } else if (x instanceof Double d) {
            return FLOAT8;
        } else if (x instanceof BigDecimal bd) {
            return NUMERIC;
        } else if (x instanceof BigInteger bi) {
            return NUMERIC;
        } else if (x instanceof BigInt bi) {
            return NUMERIC;
        } else if (x instanceof Boolean b) {
            return BOOL;
        } else if (x instanceof java.util.UUID u) {
            return UUID;
        } else if (x instanceof byte[] ba) {
            return BYTEA;
        } else if (x instanceof org.pg.json.JSON.Wrapper w) {
            return JSONB;
        } else if (x instanceof ByteBuffer bb) {
            return BYTEA;
        } else if (x instanceof Date d) {
            return TIMESTAMPTZ;
        } else if (x instanceof Instant i) {
            return TIMESTAMPTZ;
        } else if (x instanceof IPersistentMap pm) {
            return JSONB;
        } else if (x instanceof LocalTime lt) {
            return TIME;
        } else if (x instanceof OffsetTime ot) {
            return TIMETZ;
        } else if (x instanceof LocalDate ld) {
            return DATE;
        } else if (x instanceof LocalDateTime ldt) {
            return TIMESTAMP;
        } else if (x instanceof OffsetDateTime odt) {
            return TIMESTAMPTZ;
        } else if (x instanceof ZonedDateTime zdt) {
            return TIMESTAMPTZ;
        } else if (x instanceof Byte b) {
            return INT2;
        } else if (x instanceof Point p) {
            return POINT;
        } else if (x instanceof Line l) {
            return LINE;
        } else if (x instanceof Box b) {
            return BOX;
        } else if (x instanceof Circle c) {
            return CIRCLE;
        } else if (x instanceof Polygon p) {
            return POLYGON;
        } else if (x instanceof Path p) {
            return PATH;
        } else {
            return DEFAULT;
        }
    }
}
