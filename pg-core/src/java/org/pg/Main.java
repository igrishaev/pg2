package org.pg;

import clojure.lang.PersistentVector;
import clojure.lang.RT;
import org.pg.codec.CodecParams;
import org.pg.processor.IProcessor;
import org.pg.processor.Processors;
import org.pg.type.PGType;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public final class Main {

    public static void main (final String[] args) {

//        final long l = 123123123L;
//        System.out.println((short) l);

         String user = "test"; // System.getenv("USER");

        Config config = Config.builder(user, user)
                .port(10150)
                .host("127.0.0.1")
                .password(user)
                .binaryEncode(true)
                .binaryDecode(true)
                .readPGTypes(true)
                .build();
//                    public ByteBuffer encodeBin(Object value, CodecParams codecParams) {
//                        return null;
//                    }
//
//                    @Override
//                    public String encodeTxt(Object value, CodecParams codecParams) {
//                        return "";
//                    }
//
//                    @Override
//                    public Object decodeBin(ByteBuffer bb, CodecParams codecParams) {
//                        return "decodeBin";
//                    }
//
//                    @Override
//                    public Object decodeTxt(String text, CodecParams codecParams) {
//                        return "decodeTxt";
//                    }
//                }))
                .build();

//        Config config = Config.builder("test_owner", "test")
//                .port(5432)
//                .host("ep-fancy-queen-a2kw7zqr.eu-central-1.aws.neon.tech")
//                .password("")
//                .useSSL(true)
//                .build();

//        Config config = Config.builder("test_owner", "test")
//                .port(5432)
//                .host("ep-fancy-queen-a2kw7zqr.eu-central-1.aws.neon.tech")
//                .password("")
//                .useSSL(true)
//                .build();

//        Config config = Config.builder("postgres.hmtrzfggdhnofcaseomq", "postgres")
//                .port(6543)
//                .host("aws-0-eu-central-1.pooler.supabase.com")
//                .password("")
//                .useSSL(true)
//                .build();

        // Connection conn = new Connection("127.0.0.1", 15432, user, user, user);
        Connection conn = Connection.connect(config);
//        System.out.println(conn.execute("create type RGB as enum('r', 'g', 'b')"));
//        System.out.println();
//        for (PGType pgType: conn.getPGTypes()) {
//            System.out.println(pgType);
//        }

        System.out.println(conn.execute("select 'foo=>test,ab=>null,c=>42'::hstore as hs;"));
//        System.out.println(conn.execute("select '12:01:59.123456789+03'::timetz as timetz"));
//        final Object map = RT.first(conn.execute("select 1 a, 2 b, 3 c, 4 d, 5 e, 6 f, 7 g, 8 h, 9 i, 10 j, 11 k, 12 l, 13 m, 14 n, 15 o, 16 p"));
//        System.out.println(RT.seq(map));

//        System.out.println(conn.execute("select '[1,2,3]'::vector(3) as v"));

//        System.out.println(conn.resolveType("vector"));

        // System.out.println(conn.execute("create type color as enum ('R', 'G', 'B')"));

//         System.out.println(conn.execute("select '{R,G,B}'::color[] as colors"));

        System.out.println(conn.execute("select $1 as colors", ExecuteParams.builder()
                .oids(List.of("_color"))
                .params(List.of(PersistentVector.create("R", "G", "B")))
                .build()));
//        System.out.println(conn.query("select 'R'::color as color"));


        //System.out.println(conn.getId());
        //System.out.println(conn.getPid());

        // conn.query("select '{\"foo\": 555}'::jsonb as obj from generate_series(1, 10000000)");

        // System.out.println(conn.execute("select '{\"foo\": 555}'::jsonb as obj"));

        // System.out.println(conn.execute("select '1 year 1 second'::interval as interval"));

//        System.out.println(conn.execute("create temp table foo (a int, b int, c int)"));
//        InputStream in = new ByteArrayInputStream("1,2,3".getBytes());
//        conn.copy(
//                "copy foo (a, b, c) from STDIN WITH (FORMAT CSV)",
//                ExecuteParams.builder().inputStream(in).build()
//        );
//
//        System.out.println(conn.query("SELECT '\\xDEADBEEF'::bytea"));

        // SELECT E'\\xDEADBEEF';

        // TODO: fix this case!
//        conn.copy(
//                "copy foo (a, b, c) from STDIN WITH (FORMAT BINARY)",
//                ExecuteParams.builder()
//                        .copyInMaps(Collections.emptyList())
//                        .copyMapKeys(List.of(
//                                Keyword.intern("a"),
//                                Keyword.intern("b"),
//                                Keyword.intern("c")
//                        ))
//                        .setBin()
//                        .build()
//        );
//
//         Object res1 = conn.query("select x from generate_series(1, 3) as x; select 42 as foo");
//         System.out.println(res1);

        // System.out.println(conn.execute(""));

        // System.out.println(conn.execute("select 'ё'::\"char\" as char"));
        // System.out.println(conn.execute("select '{{1,2,3},{4,5,6}}'::int[][] as arr"));

//        System.out.println(conn.execute(
//                "select $1::int[] as arr",
//                ExecuteParams.builder().params(List.of(PersistentVector.create(1, 2, 3))).build())
//        );

//        String query = "select $1 as foo";
//        // "select $1::int as foo, 'test' as string, 42 as num, now() as now"
//        PreparedStatement ps = conn.prepare(query, ExecuteParams.builder().objOids(List.of(OID.BOOL)).build());
//        // List<Object> params = List.of(1);
//        Object res2 = conn.executeStatement(ps, ExecuteParams.builder().params(List.of(true)).build());
//        conn.closeStatement(ps);
        // System.out.println(res2.toString());

          //Object res3 = conn.execute("select 'ёёёё'::char as char");
          //System.out.println(res3);

//        Object res4 = conn.execute(
//                "select $1::char as char",
//                ExecuteParams.builder().params('ё').build()
//        );
//        System.out.println(res4);

//        conn.query("create type type_foo as enum ('foo', 'bar', 'kek')");
//        conn.query("create table aaa (id integer, foo type_foo)");
//        conn.query("insert into aaa values (1, 'foo'), (2, 'bar')");
//        System.out.println(conn.execute(
//                "select * from aaa",
//                ExecuteParams.builder().binaryDecode(true).build()
//                )
//        );

        // conn.execute("create table abc (id integer, title text)");
//        Object resIns = conn.execute(
//                "insert into abc (id, title) values ($1, $2), ($3, $4) returning *",
//                new ExecuteParams.Builder().params(1, "test2", 2, "test2").build()
//        );
//
//        System.out.println(resIns);
//
//        Object res4 = conn.query("copy (select s.x as x, s.x * s.x as square from generate_series(1, 9) as s(x)) TO STDOUT WITH (FORMAT CSV)");
//        System.out.println(res4);
//
//        FileOutputStream out;
//        try {
//            out = new FileOutputStream("foo.csv");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        Object res5 = conn.copyOut("copy (select s.x as x, s.x * s.x as square from generate_series(1, 9) as s(x)) TO STDOUT WITH (FORMAT CSV)", out);
//        System.out.println(res5);
//
//        Object res6 = conn.query("select '[1, 2, 3, {\"foo/bar\": true}]'::jsonb");
//        System.out.println(res6);
//
//        Object res7 = conn.execute("select '[1, 2, 3, {\"foo/bar\": true}]'::jsonb");
//        System.out.println(res7);



        // System.out.println(SourceType.STATEMENT.getCode());
    }
}
