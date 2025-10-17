package org.pg.processor;

import org.pg.enums.OID;
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

    public record Node (int oid, IProcessor processor) {}

    public static class MyMap {
        final static int RANGE = 428;
        final static int SPACE = 2;
        final private Node[][] cells;

        public MyMap() {
            cells = new Node[RANGE][SPACE];
        }

        public void set(final int oid, final IProcessor processor) {
            final int i = oid % RANGE;
            final Node[] list = cells[i];
            int j;
            for (j = 0; j < list.length; j++) {
                if (list[j] == null || list[j].oid == oid) {
                    break;
                }
            }
            if (j == list.length) {
                throw new RuntimeException("list is full");
            }
            list[j] = new Node(oid, processor);
        }
        public IProcessor get(final int oid) {
            if (oid < 0) return null;
            final int i = oid % RANGE;
            Node[] list = cells[i];
            for (int j = 0; j < list.length; j++) {
                if (list[j] == null) {
                    return null;
                } else if (list[j].oid == oid) {
                    return list[j].processor;
                }
            }
            return null;
        }
    }

    final static MyMap mm = new MyMap();
    static {
        // numbers
        mm.set(OID.INT2, new Int2());
        mm.set(OID.INT4, new Int4());
        mm.set(OID.OID, new Int4());
        mm.set(OID.INT8, new Int8());
        mm.set(OID.NUMERIC, new Numeric());
        mm.set(OID.FLOAT4, new Float4());
        mm.set(OID.FLOAT8, new Float8());
        // text
        mm.set(OID.VARCHAR, new Text(OID.VARCHAR));
        mm.set(OID.TEXT, new Text(OID.TEXT));
        mm.set(OID.NAME, new Text(OID.NAME));
        mm.set(OID.BPCHAR, new Text(OID.BPCHAR));
        mm.set(OID.REGPROC, new Text(OID.REGPROC));

        mm.set(OID.CHAR, new Char());
        // geometry
        mm.set(OID.POINT, new Point());
        mm.set(OID.LINE, new Line());
        mm.set(OID.BOX, new Box());
        mm.set(OID.CIRCLE, new Circle());
        mm.set(OID.POLYGON, new Polygon());
        mm.set(OID.PATH, new Path());
        mm.set(OID.LSEG, new LineSegment());
        // misc
        mm.set(OID.UUID, new Uuid());
        mm.set(OID.JSON, new Json());
        mm.set(OID.JSONB, new Jsonb());
        mm.set(OID.BYTEA, new Bytea());
        mm.set(OID.BOOL, new Bool());
        mm.set(OID.BIT, new Bit());
        // date & time
        mm.set(OID.TIMESTAMPTZ, new Timestamptz());
        mm.set(OID.TIMESTAMP, new Timestamp());
        mm.set(OID.DATE, new Date());
        mm.set(OID.TIME, new Time());
        mm.set(OID.TIMETZ, new Timetz());
        // arrays
        mm.set(OID._INT2, new Array(OID._INT2, OID.INT2));
        mm.set(OID._INT4, new Array(OID._INT4, OID.INT4));
        mm.set(OID._OID, new Array(OID._OID, OID.OID));
        mm.set(OID._INT8, new Array(OID._INT8, OID.INT8));
        mm.set(OID._NUMERIC, new Array(OID._NUMERIC, OID.NUMERIC));
        mm.set(OID._FLOAT4, new Array(OID._FLOAT4, OID.FLOAT4));
        mm.set(OID._FLOAT8, new Array(OID._FLOAT8, OID.FLOAT8));

        mm.set(OID._INT2, new Array(OID._INT2, OID.INT2));
        mm.set(OID._INT4, new Array(OID._INT4, OID.INT4));
        mm.set(OID._OID, new Array(OID._OID, OID.OID));
        mm.set(OID._INT8, new Array(OID._INT8, OID.INT8));
        mm.set(OID._NUMERIC, new Array(OID._NUMERIC, OID.NUMERIC));
        mm.set(OID._FLOAT4, new Array(OID._FLOAT4, OID.FLOAT4));
        mm.set(OID._FLOAT8, new Array(OID._FLOAT8, OID.FLOAT8));
        mm.set(OID._VARCHAR, new Array(OID._VARCHAR, OID.VARCHAR));
        mm.set(OID._TEXT, new Array(OID._TEXT, OID.TEXT));
        mm.set(OID._NAME, new Array(OID._NAME, OID.NAME));
        mm.set(OID._BPCHAR, new Array(OID._BPCHAR, OID.BPCHAR));
        mm.set(OID._CHAR, new Array(OID._CHAR, OID.CHAR));
        mm.set(OID._UUID, new Array(OID._UUID, OID.UUID));
        mm.set(OID._JSON, new Array(OID._JSON, OID.JSON));
        mm.set(OID._JSONB, new Array(OID._JSONB, OID.JSONB));
        mm.set(OID._BYTEA, new Array(OID._BYTEA, OID.BYTEA));
        mm.set(OID._BOOL, new Array(OID._BOOL, OID.BOOL));
        mm.set(OID._TIMESTAMPTZ, new Array(OID._TIMESTAMPTZ, OID.TIMESTAMPTZ));
        mm.set(OID._TIMESTAMP, new Array(OID._TIMESTAMP, OID.TIMESTAMP));
        mm.set(OID._DATE, new Array(OID._DATE, OID.DATE));
        mm.set(OID._TIME, new Array(OID._TIME, OID.TIME));
        mm.set(OID._TIMETZ, new Array(OID._TIMETZ, OID.TIMETZ));
        mm.set(OID._BIT, new Array(OID._BIT, OID.BIT));
        mm.set(OID._POINT, new Array(OID._POINT, OID.POINT));
        mm.set(OID._LINE, new Array(OID._LINE, OID.LINE));
        mm.set(OID._BOX, new Array(OID._BOX, OID.BOX));
        mm.set(OID._CIRCLE, new Array(OID._CIRCLE, OID.CIRCLE));
        mm.set(OID._POLYGON, new Array(OID._POLYGON, OID.POLYGON));
        mm.set(OID._PATH, new Array(OID._PATH, OID.PATH));
        mm.set(OID._LSEG, new Array(OID._LSEG, OID.LSEG));
        mm.set(OID._REGPROC, new Array(OID._REGPROC, OID.REGPROC));
    }

    public static IProcessor getProcessor(final int oid) {
        return mm.get(oid);
    }

    public static void main(final String... args) {
        final MyMap mm = new MyMap();
        mm.set(21, unsupported);
        System.out.println(mm.get(21));
        mm.set(21, null);
        System.out.println(mm.get(21));

    }
}
