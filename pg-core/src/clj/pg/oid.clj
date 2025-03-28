(ns pg.oid
  "
  A dedicated namespace with native (built-in) Postgres OID types.
  "
  (:refer-clojure :exclude [char name time])
  (:import
   org.pg.enums.OID))

(def ^int default OID/DEFAULT)
(def ^int bool OID/BOOL)
(def ^int _bool OID/_BOOL)
(def ^int bytea OID/BYTEA)
(def ^int _bytea OID/_BYTEA)
(def ^int char OID/CHAR)
(def ^int _char OID/_CHAR)
(def ^int name OID/NAME)
(def ^int _name OID/_NAME)
(def ^int int8 OID/INT8)
(def ^int _int8 OID/_INT8)
(def ^int int2 OID/INT2)
(def ^int _int2 OID/_INT2)
(def ^int int2vector OID/INT2VECTOR)
(def ^int _int2vector OID/_INT2VECTOR)
(def ^int int4 OID/INT4)
(def ^int _int4 OID/_INT4)
(def ^int regproc OID/REGPROC)
(def ^int _regproc OID/_REGPROC)
(def ^int text OID/TEXT)
(def ^int _text OID/_TEXT)
(def ^int oid OID/OID)
(def ^int _oid OID/_OID)
(def ^int tid OID/TID)
(def ^int _tid OID/_TID)
(def ^int xid OID/XID)
(def ^int _xid OID/_XID)
(def ^int cid OID/CID)
(def ^int _cid OID/_CID)
(def ^int oidvector OID/OIDVECTOR)
(def ^int _oidvector OID/_OIDVECTOR)
(def ^int pg_type OID/PG_TYPE)
(def ^int _pg_type OID/_PG_TYPE)
(def ^int pg_attribute OID/PG_ATTRIBUTE)
(def ^int _pg_attribute OID/_PG_ATTRIBUTE)
(def ^int pg_proc OID/PG_PROC)
(def ^int _pg_proc OID/_PG_PROC)
(def ^int pg_class OID/PG_CLASS)
(def ^int _pg_class OID/_PG_CLASS)
(def ^int json OID/JSON)
(def ^int _json OID/_JSON)
(def ^int xml OID/XML)
(def ^int _xml OID/_XML)
(def ^int pg_node_tree OID/PG_NODE_TREE)
(def ^int pg_ndistinct OID/PG_NDISTINCT)
(def ^int pg_dependencies OID/PG_DEPENDENCIES)
(def ^int pg_mcv_list OID/PG_MCV_LIST)
(def ^int pg_ddl_command OID/PG_DDL_COMMAND)
(def ^int xid8 OID/XID8)
(def ^int _xid8 OID/_XID8)
(def ^int point OID/POINT)
(def ^int _point OID/_POINT)
(def ^int lseg OID/LSEG)
(def ^int _lseg OID/_LSEG)
(def ^int path OID/PATH)
(def ^int _path OID/_PATH)
(def ^int box OID/BOX)
(def ^int _box OID/_BOX)
(def ^int polygon OID/POLYGON)
(def ^int _polygon OID/_POLYGON)
(def ^int line OID/LINE)
(def ^int _line OID/_LINE)
(def ^int float4 OID/FLOAT4)
(def ^int _float4 OID/_FLOAT4)
(def ^int float8 OID/FLOAT8)
(def ^int _float8 OID/_FLOAT8)
(def ^int unknown OID/UNKNOWN)
(def ^int circle OID/CIRCLE)
(def ^int _circle OID/_CIRCLE)
(def ^int money OID/MONEY)
(def ^int _money OID/_MONEY)
(def ^int macaddr OID/MACADDR)
(def ^int _macaddr OID/_MACADDR)
(def ^int inet OID/INET)
(def ^int _inet OID/_INET)
(def ^int cidr OID/CIDR)
(def ^int _cidr OID/_CIDR)
(def ^int macaddr8 OID/MACADDR8)
(def ^int _macaddr8 OID/_MACADDR8)
(def ^int aclitem OID/ACLITEM)
(def ^int _aclitem OID/_ACLITEM)
(def ^int bpchar OID/BPCHAR)
(def ^int _bpchar OID/_BPCHAR)
(def ^int varchar OID/VARCHAR)
(def ^int _varchar OID/_VARCHAR)
(def ^int date OID/DATE)
(def ^int _date OID/_DATE)
(def ^int time OID/TIME)
(def ^int _time OID/_TIME)
(def ^int timestamp OID/TIMESTAMP)
(def ^int _timestamp OID/_TIMESTAMP)
(def ^int timestamptz OID/TIMESTAMPTZ)
(def ^int _timestamptz OID/_TIMESTAMPTZ)
(def ^int interval OID/INTERVAL)
(def ^int _interval OID/_INTERVAL)
(def ^int timetz OID/TIMETZ)
(def ^int _timetz OID/_TIMETZ)
(def ^int bit OID/BIT)
(def ^int _bit OID/_BIT)
(def ^int varbit OID/VARBIT)
(def ^int _varbit OID/_VARBIT)
(def ^int numeric OID/NUMERIC)
(def ^int _numeric OID/_NUMERIC)
(def ^int refcursor OID/REFCURSOR)
(def ^int _refcursor OID/_REFCURSOR)
(def ^int regprocedure OID/REGPROCEDURE)
(def ^int _regprocedure OID/_REGPROCEDURE)
(def ^int regoper OID/REGOPER)
(def ^int _regoper OID/_REGOPER)
(def ^int regoperator OID/REGOPERATOR)
(def ^int _regoperator OID/_REGOPERATOR)
(def ^int regclass OID/REGCLASS)
(def ^int _regclass OID/_REGCLASS)
(def ^int regcollation OID/REGCOLLATION)
(def ^int _regcollation OID/_REGCOLLATION)
(def ^int regtype OID/REGTYPE)
(def ^int _regtype OID/_REGTYPE)
(def ^int regrole OID/REGROLE)
(def ^int _regrole OID/_REGROLE)
(def ^int regnamespace OID/REGNAMESPACE)
(def ^int _regnamespace OID/_REGNAMESPACE)
(def ^int uuid OID/UUID)
(def ^int _uuid OID/_UUID)
(def ^int pg_lsn OID/PG_LSN)
(def ^int _pg_lsn OID/_PG_LSN)
(def ^int tsvector OID/TSVECTOR)
(def ^int _tsvector OID/_TSVECTOR)
(def ^int gtsvector OID/GTSVECTOR)
(def ^int _gtsvector OID/_GTSVECTOR)
(def ^int tsquery OID/TSQUERY)
(def ^int _tsquery OID/_TSQUERY)
(def ^int regconfig OID/REGCONFIG)
(def ^int _regconfig OID/_REGCONFIG)
(def ^int regdictionary OID/REGDICTIONARY)
(def ^int _regdictionary OID/_REGDICTIONARY)
(def ^int jsonb OID/JSONB)
(def ^int _jsonb OID/_JSONB)
(def ^int jsonpath OID/JSONPATH)
(def ^int _jsonpath OID/_JSONPATH)
(def ^int txid_snapshot OID/TXID_SNAPSHOT)
(def ^int _txid_snapshot OID/_TXID_SNAPSHOT)
(def ^int pg_snapshot OID/PG_SNAPSHOT)
(def ^int _pg_snapshot OID/_PG_SNAPSHOT)
(def ^int int4range OID/INT4RANGE)
(def ^int _int4range OID/_INT4RANGE)
(def ^int numrange OID/NUMRANGE)
(def ^int _numrange OID/_NUMRANGE)
(def ^int tsrange OID/TSRANGE)
(def ^int _tsrange OID/_TSRANGE)
(def ^int tstzrange OID/TSTZRANGE)
(def ^int _tstzrange OID/_TSTZRANGE)
(def ^int daterange OID/DATERANGE)
(def ^int _daterange OID/_DATERANGE)
(def ^int int8range OID/INT8RANGE)
(def ^int _int8range OID/_INT8RANGE)
(def ^int int4multirange OID/INT4MULTIRANGE)
(def ^int _int4multirange OID/_INT4MULTIRANGE)
(def ^int nummultirange OID/NUMMULTIRANGE)
(def ^int _nummultirange OID/_NUMMULTIRANGE)
(def ^int tsmultirange OID/TSMULTIRANGE)
(def ^int _tsmultirange OID/_TSMULTIRANGE)
(def ^int tstzmultirange OID/TSTZMULTIRANGE)
(def ^int _tstzmultirange OID/_TSTZMULTIRANGE)
(def ^int datemultirange OID/DATEMULTIRANGE)
(def ^int _datemultirange OID/_DATEMULTIRANGE)
(def ^int int8multirange OID/INT8MULTIRANGE)
(def ^int _int8multirange OID/_INT8MULTIRANGE)
(def ^int record OID/RECORD)
(def ^int _record OID/_RECORD)
(def ^int cstring OID/CSTRING)
(def ^int _cstring OID/_CSTRING)
(def ^int any OID/ANY)
(def ^int anyarray OID/ANYARRAY)
(def ^int void OID/VOID)
(def ^int trigger OID/TRIGGER)
(def ^int event_trigger OID/EVENT_TRIGGER)
(def ^int language_handler OID/LANGUAGE_HANDLER)
(def ^int internal OID/INTERNAL)
(def ^int anyelement OID/ANYELEMENT)
(def ^int anynonarray OID/ANYNONARRAY)
(def ^int anyenum OID/ANYENUM)
(def ^int fdw_handler OID/FDW_HANDLER)
(def ^int index_am_handler OID/INDEX_AM_HANDLER)
(def ^int tsm_handler OID/TSM_HANDLER)
(def ^int table_am_handler OID/TABLE_AM_HANDLER)
(def ^int anyrange OID/ANYRANGE)
(def ^int anycompatible OID/ANYCOMPATIBLE)
(def ^int anycompatiblearray OID/ANYCOMPATIBLEARRAY)
(def ^int anycompatiblenonarray OID/ANYCOMPATIBLENONARRAY)
(def ^int anycompatiblerange OID/ANYCOMPATIBLERANGE)
(def ^int anymultirange OID/ANYMULTIRANGE)
(def ^int anycompatiblemultirange OID/ANYCOMPATIBLEMULTIRANGE)
(def ^int pg_brin_bloom_summary OID/PG_BRIN_BLOOM_SUMMARY)
(def ^int pg_brin_minmax_multi_summary OID/PG_BRIN_MINMAX_MULTI_SUMMARY)
