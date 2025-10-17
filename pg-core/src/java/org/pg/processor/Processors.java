package org.pg.processor;

import org.pg.enums.OID;
import org.pg.error.PGError;
import org.pg.processor.pgvector.Sparsevec;
import org.pg.processor.pgvector.Vector;

public class Processors {

    public static IProcessor unsupported = new Unsupported();
    public static IProcessor defaultEnum = new Enum();
    public static IProcessor vector = new Vector();
    public static IProcessor sparsevec = new Sparsevec();
    public static IProcessor hstore = new Hstore();

    public static boolean isKnownOid(final int oid) {
        return getProcessor(oid) != null;
    }

    // just a bit faster than HashMap :)
    public static class ToyMap {

        private record Node (int oid, IProcessor processor) {}

        final static int RANGE = 91;
        final static int SPACE = 3;
        final private Node[][] cells;

        public ToyMap() {
            cells = new Node[RANGE][SPACE];
        }

        public void set(final int oid, final IProcessor processor) {
            final int i = oid % RANGE;
            final Node[] list = cells[i];
            int j;
            for (j = 0; j < SPACE; j++) {
                if (list[j] == null || list[j].oid == oid) {
                    break;
                }
            }
            if (j == list.length) {
                throw new PGError("The node list is full, oid: %s, space: %s",
                        oid, SPACE);
            }
            list[j] = new Node(oid, processor);
        }
        public IProcessor get(final int oid) {
            if (oid < 0) return null;
            final int i = oid % RANGE;
            Node[] list = cells[i];
            for (int j = 0; j < SPACE; j++) {
                if (list[j] == null) {
                    return null;
                } else if (list[j].oid == oid) {
                    return list[j].processor;
                }
            }
            return null;
        }
    }

    final static ToyMap oidMap = new ToyMap();
    static {
        // numbers
        oidMap.set(OID.INT2, new Int2());
        oidMap.set(OID.INT4, new Int4());
        oidMap.set(OID.OID, new Int4());
        oidMap.set(OID.INT8, new Int8());
        oidMap.set(OID.NUMERIC, new Numeric());
        oidMap.set(OID.FLOAT4, new Float4());
        oidMap.set(OID.FLOAT8, new Float8());
        // text
        oidMap.set(OID.VARCHAR, new Text(OID.VARCHAR));
        oidMap.set(OID.TEXT, new Text(OID.TEXT));
        oidMap.set(OID.NAME, new Text(OID.NAME));
        oidMap.set(OID.BPCHAR, new Text(OID.BPCHAR));
        oidMap.set(OID.REGPROC, new Text(OID.REGPROC));

        oidMap.set(OID.CHAR, new Char());
        // geometry
        oidMap.set(OID.POINT, new Point());
        oidMap.set(OID.LINE, new Line());
        oidMap.set(OID.BOX, new Box());
        oidMap.set(OID.CIRCLE, new Circle());
        oidMap.set(OID.POLYGON, new Polygon());
        oidMap.set(OID.PATH, new Path());
        oidMap.set(OID.LSEG, new LineSegment());
        // misc
        oidMap.set(OID.UUID, new Uuid());
        oidMap.set(OID.JSON, new Json());
        oidMap.set(OID.JSONB, new Jsonb());
        oidMap.set(OID.BYTEA, new Bytea());
        oidMap.set(OID.BOOL, new Bool());
        oidMap.set(OID.BIT, new Bit());
        // date & time
        oidMap.set(OID.TIMESTAMPTZ, new Timestamptz());
        oidMap.set(OID.TIMESTAMP, new Timestamp());
        oidMap.set(OID.DATE, new Date());
        oidMap.set(OID.TIME, new Time());
        oidMap.set(OID.TIMETZ, new Timetz());
        // arrays
        oidMap.set(OID._INT2, new Array(OID._INT2, OID.INT2));
        oidMap.set(OID._INT4, new Array(OID._INT4, OID.INT4));
        oidMap.set(OID._OID, new Array(OID._OID, OID.OID));
        oidMap.set(OID._INT8, new Array(OID._INT8, OID.INT8));
        oidMap.set(OID._NUMERIC, new Array(OID._NUMERIC, OID.NUMERIC));
        oidMap.set(OID._FLOAT4, new Array(OID._FLOAT4, OID.FLOAT4));
        oidMap.set(OID._FLOAT8, new Array(OID._FLOAT8, OID.FLOAT8));

        oidMap.set(OID._INT2, new Array(OID._INT2, OID.INT2));
        oidMap.set(OID._INT4, new Array(OID._INT4, OID.INT4));
        oidMap.set(OID._OID, new Array(OID._OID, OID.OID));
        oidMap.set(OID._INT8, new Array(OID._INT8, OID.INT8));
        oidMap.set(OID._NUMERIC, new Array(OID._NUMERIC, OID.NUMERIC));
        oidMap.set(OID._FLOAT4, new Array(OID._FLOAT4, OID.FLOAT4));
        oidMap.set(OID._FLOAT8, new Array(OID._FLOAT8, OID.FLOAT8));
        oidMap.set(OID._VARCHAR, new Array(OID._VARCHAR, OID.VARCHAR));
        oidMap.set(OID._TEXT, new Array(OID._TEXT, OID.TEXT));
        oidMap.set(OID._NAME, new Array(OID._NAME, OID.NAME));
        oidMap.set(OID._BPCHAR, new Array(OID._BPCHAR, OID.BPCHAR));
        oidMap.set(OID._CHAR, new Array(OID._CHAR, OID.CHAR));
        oidMap.set(OID._UUID, new Array(OID._UUID, OID.UUID));
        oidMap.set(OID._JSON, new Array(OID._JSON, OID.JSON));
        oidMap.set(OID._JSONB, new Array(OID._JSONB, OID.JSONB));
        oidMap.set(OID._BYTEA, new Array(OID._BYTEA, OID.BYTEA));
        oidMap.set(OID._BOOL, new Array(OID._BOOL, OID.BOOL));
        oidMap.set(OID._TIMESTAMPTZ, new Array(OID._TIMESTAMPTZ, OID.TIMESTAMPTZ));
        oidMap.set(OID._TIMESTAMP, new Array(OID._TIMESTAMP, OID.TIMESTAMP));
        oidMap.set(OID._DATE, new Array(OID._DATE, OID.DATE));
        oidMap.set(OID._TIME, new Array(OID._TIME, OID.TIME));
        oidMap.set(OID._TIMETZ, new Array(OID._TIMETZ, OID.TIMETZ));
        oidMap.set(OID._BIT, new Array(OID._BIT, OID.BIT));
        oidMap.set(OID._POINT, new Array(OID._POINT, OID.POINT));
        oidMap.set(OID._LINE, new Array(OID._LINE, OID.LINE));
        oidMap.set(OID._BOX, new Array(OID._BOX, OID.BOX));
        oidMap.set(OID._CIRCLE, new Array(OID._CIRCLE, OID.CIRCLE));
        oidMap.set(OID._POLYGON, new Array(OID._POLYGON, OID.POLYGON));
        oidMap.set(OID._PATH, new Array(OID._PATH, OID.PATH));
        oidMap.set(OID._LSEG, new Array(OID._LSEG, OID.LSEG));
        oidMap.set(OID._REGPROC, new Array(OID._REGPROC, OID.REGPROC));
    }

    public static IProcessor getProcessor(final int oid) {
        return oidMap.get(oid);
    }

    public static void main(final String... args) {
        final ToyMap tm = new ToyMap();
        tm.set(21, unsupported);
        System.out.println(tm.get(21));
        tm.set(21, null);
        System.out.println(tm.get(21));

    }
}
