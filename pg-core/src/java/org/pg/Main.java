package org.pg;

import clojure.lang.Keyword;
import clojure.lang.PersistentVector;
import org.pg.enums.OID;

import java.util.List;

public final class Main {

    public static void main (final String[] args) {

//        final long l = 123123123L;
//        System.out.println((short) l);

//         String user = System.getenv("USER");

        String user = "test";

        Config config = Config.builder(user, user)
                .port(10150)
                .host("127.0.0.1")
                .password(user)
                .binaryEncode(true)
                .binaryDecode(true)
                .build();

//        Config config = Config.builder("test_owner", "test")
//                .port(5432)
//                .host("ep-fancy-queen-a2kw7zqr.eu-central-1.aws.neon.tech")
//                .password("")
//                .useSSL(true)
//                .build();

        // Connection conn = new Connection("127.0.0.1", 15432, user, user, user);
        Connection conn = Connection.connect(config);
//        System.out.println(conn.execute("select '[1,2,3]'::vector(3) as v"));

        ExecuteParams params = ExecuteParams.builder()
                .params(List.of(
                        PersistentVector.create(1,2,3),
                        PersistentVector.create(1,2,3)
                ))
                .OIDs(List.of(
                        "public.vector",
//                        Keyword.intern("public", "vector"),
                        Keyword.intern("public", "vector")
                ))
                .build();
        System.out.println(conn.execute("select $1 as v1, $2 as v2", params));


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
