# Hstore Support

[hstore]: https://postgrespro.com/docs/postgresql/15/hstore

PG2 supports the official "hstore" extension which Postgres ships out from the
box. The type `hstore` provides a set of text key/value pairs, for example:

~~~sql
foo => bar, baz => whatever
~~~

which maps to a Clojure map:

~~~clojure
{:foo "bar", :baz "whatever"}
~~~

Why using `hstore` if there is `json(b)` which is much more powerful? Although
`hstore` supports text values only, it's about 10% faster than `jsonb`. And
sometimes, `hstore` is just enough.

Install the extension as follows:

~~~sql
create extension if not exists hstore;
~~~

Here is a beief demo of how to use this type:

~~~clojure

(def conn (pg/connect ...))

(pg/execute conn "create temp table test (id int, hs hstore)")

(pg/execute conn
            "insert into test (id, hs) values ($1, $2), ($3, $4)"
            {:params [1 {:foo 1 :bar 2 :test "3"}
                      2 {"a" nil "c" "test"}]})

(def result
  (pg/execute conn "select * from test order by id"))

;; [{:id 1, :hs {:bar "2", :foo "1", :test "3"}}
;;  {:id 2, :hs {:c "test", :a nil}}]
~~~

If you `COPY...FROM` data into a table that has an `hstore` column, use type
hint:

~~~clojure
(def rows
  [[1 nil]
   [2 {}]
   [3 {nil 1 "" 2 "test" 3}]
   [4 {"foo" nil "bar" "" :baz 3}]
   [5 {1 "test" 'hey 2 :foo :bar}]
   [6 {:foo true :bar 'test :test/baz :lol/kek false "test"}]])

(pg/copy-in-rows conn
                 "copy foo (id, hs) from STDIN WITH (FORMAT CSV)"
                 rows
                 {:oids [pg.oid/int4 :hstore]})
~~~

This is because by default, a Clojure map is considered as a JSON value. See
[Type Hints (OIDs)](/docs/oids-hints.md) for more info.

Hstore values are read to Clojure maps and vice versa. The `hstore` type allows
keys to be empty strings but not null. Values can be null.

When encoding a Clojure map, the following rules apply for keys:

- if a key is null, it becomes an empty string to prevent an exception;
- keyword keys are written without a leading colon,
- for keywords and symbols, namespace is preserved;
- keys of other types are coerced to strings;
- during encoding, all keys are turned into keywords back.

Rules for values:

- keywords are written without a leading colon so you can safely transform them
  to keywords back;
- nil is written as a `NULL` literal;
- anything else is transformed to a string.
