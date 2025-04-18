## Type Hints (OIDs)

When you perform `execute` or `copy` methods, there is a special option called
`:oids` available. It's a list of OIDs which helps Postgres to derive types of
parameters:

~~~clojure
(ns foo.bar
  (:require
    [pg.core :as pg]
    [pg.oid :as oid]))

(pg/execute conn
            "insert into test (id, foo) values ($1, $2)"
            {:params [3 "kek"]
             :oids [oid/int8 oid/text]})
~~~

When not passed, OIDs are derived by Postgres. In rare cases though you can tell
it: hey, this parameter must be this but not that. It could be done either on
SQL level by adding an explicit type cast: `... $1::int`. Another option is to
pass a list of OIDs.

For built-in types, use constants declared in the `pg.oid` namespace. These are
taken from Postgres source code:

~~~clojure
(ns pg.oid
  (:import
   org.pg.enums.OID))

(def ^int default OID/DEFAULT)
(def ^int bool    OID/BOOL)
(def ^int bytea   OID/BYTEA)
(def ^int char    OID/CHAR)
...
~~~

But if you want to reference a type from an extension or which is a enum, you
don't know its OID. Specify either a string or a `clojure.lang.Named` instance
(either a keyword or a symbol):

~~~clojure
{:oids ["vector"]}
;; or
{:oids [:vector]}
~~~

Above, both type hints are specified without a schema. The default schema is
"public". Under the hood, they are transferred into "public.vector". To specify
a schema explicitly, use a dot notation for a string. For a keyword, provide a
namespace:

~~~clojure
{:oids ["public.vector"]}
;; or
{:oids [:public/vector]}
~~~

When a type is not built-in, PG2 performs an extra query to fetch its metadata:

~~~sql
copy (
    select
        pg_type.oid,
        pg_type.typname,
        pg_type.typtype,
        pg_type.typinput::text,
        pg_type.typoutput::text,
        pg_type.typreceive::text,
        pg_type.typsend::text,
        pg_type.typarray,
        pg_type.typdelim,
        pg_type.typelem,
        pg_namespace.nspname
    from
        pg_type
    join
        pg_namespace on pg_type.typnamespace = pg_namespace.oid
    where
        pg_namespace.nspname = $$public$$ and pg_type.typname = $$vector$$
) to stdout
    with (format binary)
~~~

This behavior is described in the [Reading Types In
Runtime](/docs/read-pg-types.md) section.

When no such type was found, PG2 throws an exception.

Another way to reference a non-standard type is to fetch its OID using the
`pg/oid` function:

~~~clojure
(pg/oid conn :vector)
;; 16423
~~~

You can save this OID into a local variable and pass it into the `:oids` list.

The following example shows how to `COPY` rows where one of the columns is of
the `vector` type (see the [PGVector Support](/docs/pgvector.md) section):

~~~clojure
(pg/query conn "create temp table foo (id int, v vector)")

(let [rows
      [[100, [1 2 3]]
       [200, [4 5 6]]]

      oid-vector
      (pg/oid conn :vector)]

  (pg/copy-in-rows conn
                   "copy foo (v) from STDIN WITH (FORMAT CSV)"
                   rows
                   {:oids [oid/int4, oid-vector]}))
~~~

This exact case would not work without passing `:oids` explicitly. By default,
PG2 doesn't know that vectors `[1 2 3]` and `[4 5 6]` should be encoded as
`vector` values from the `pg_vector` extension. But with an explicit oid, it
does.

Explicit OIDs are rarely used; most likely you will never need them. But
sometimes they help a lot.
