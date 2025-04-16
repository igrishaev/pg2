TODO

# PGVector Support

[pgvector]: https://github.com/pgvector/pgvector

Pgvector is a [well known extension][pgvector] for PostgreSQL. It provides a
fast and robust vector type which is quite useful for heavy
computations. Pgvector also provides a sparse version of a vector to save space.

This section covers how to use types provided by the extension with PG2.

## Vector

First, install `pgvector` as the official readme file prescribes. Now that you
have it installed, try a simple table with the `vector` column:

~~~clojure
(def conn
  (pg/connect {...}))

(pg/query conn "create temp table test (id int, items vector)")

(pg/execute conn "insert into test values (1, '[1,2,3]')")
(pg/execute conn "insert into test values (2, '[1,2,3,4,5]')")

(pg/execute conn "select * from test order by id")

;; [{:id 1, :items "[1,2,3]"} {:id 2, :items "[1,2,3,4,5]"}]
~~~

It works, but we got the result unparsed: the `:items` field in each row is a
string. This is because, to take a custom type into account when encoding and
decoding data, you need to specify something. Namely, pass the `:with-pgvector?`
flag to the config map as follows:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 5432
   :user "test"
   :password "test"
   :database "test"
   :with-pgvector? true})

(def conn
  (pg/connect config))
~~~

Now the strings are parsed into a Clojure vector of `double` values:

~~~clojure
(pg/execute conn "select * from test order by id")

[{:id 1, :items [1.0 2.0 3.0]}
 {:id 2, :items [1.0 2.0 3.0 4.0 5.0]}]
~~~

To insert a vector, pass it as a Clojure vector as well:

~~~clojure
(pg/execute conn "insert into test values ($1, $2)"
            {:params [3 [1 2 3 4 5]]})
~~~

It can be also a lazy collection of numbers produced by a `map` call:

~~~clojure
(pg/execute conn "insert into test values ($1, $2)"
            {:params [4 (map inc [1 2 3 4 5])]})
~~~

The `vector` column above doesn't have an explicit size. Thus, vectors of any
size can be stored in that column. You can limit the size by providing it in
parentheses:

~~~clojure
(pg/query conn "create temp table test2 (id int, items vector(5))")
~~~

Now if you pass a vector of a different size, you'll get an error response from
the database:

~~~clojure
(pg/execute conn "insert into test2 values (1, '[1,2,3]')")

;; Server error response: {severity=ERROR, code=22000, file=vector.c, line=77,
;; function=CheckExpectedDim, message=expected 5 dimensions, not 3,
;; verbosity=ERROR}
~~~

The `vector` type supports both text and binary modes of PostgreSQL wire
protocol.

## Sparse Vector

The `pgvector` extension provides a special `sparsevec` type to store vectors
where only certain elements are filled. All the rest elements are considered as
zero. For example, you have a vector of 1000 items where the 3rd item is 42.001,
and 10th item is 99.123. Storing it as a native vector of 1000 double numbers is
inefficient. It can be written as follows which takes much less:

~~~
{3:42.001,10:99.123}/1000
~~~

The `sparsevec` Postgres type acts exactly like this: internally, it's a sort of
a map that stores the size (1000) and the `{index -> value}` mapping. An
important note is that **indexes are counted from one, not zero** (see the
README.md file of the extension for details).

PG2 provides a special wrapper for a sparse vector. A brief demo:

~~~clojure
(pg/execute conn "create temp table test3 (id int, v sparsevec)")

(pg/execute conn "insert into test3 values (1, '{2:42.00001,7:99.00009}/9')")

(pg/execute conn "select * from test3")

;; [{:v <SparseVector {2:42.00001,7:99.00009}/9>, :id 1}]
~~~

The `v` field above is an instance of the `org.pg.type.SparseVector`
class. Let's look at it closer:

~~~clojure
;; put it into a separate variable
(def -sv
  (-> (pg/execute conn "select * from test3")
      first
      :v))

(type -sv)

org.pg.type.SparseVector
~~~

The `-sv` value has a number of interesting traits. To turn in into a native
Clojure map, just `deref` it:

~~~clojure
@-sv

{:nnz 2, :index {1 42.00001, 6 99.00009}, :dim 9}
~~~

It mimics the `nth` access as the standard Clojure vector does:

~~~clojure
(nth -sv 0) ;; 0.0
(nth -sv 1) ;; 42.00001
(nth -sv 2) ;; 0.0
~~~

To turn in into a native vector, just pass it into the `vec` function:

~~~clojure
(vec -sv)

[0.0 42.00001 0.0 0.0 0.0 0.0 99.00009 0.0 0.0]
~~~

There are several ways you can insert a sparse vector into the database. First,
pass an ordinary vector:

~~~clojure
(pg/execute conn "insert into test3 values ($1, $2)"
            {:params [2 [5 2 6 0 2 5 0 0]]})
~~~

Internally, zero values get eliminated, and the vector is transformed into a
`SparseVector` instance. Now read it back:

~~~clojure
(pg/execute conn "select * from test3 where id = 2")

[{:v <SparseVector {1:5.0,2:2.0,3:6.0,5:2.0,6:5.0}/8>, :id 2}]
~~~

The second way is to pass a `SparseVector` instance produced by the
`pg.type/->sparse-vector` function. It accepts the size of the vector and a
mapping of `{index => value}`:

~~~clojure
(require '[pg.type :as t])

(pg/execute conn "insert into test3 values ($1, $2)"
            {:params [3 (t/->sparse-vector 9 {0 523.23423
                                              7 623.52346})]})
~~~

Finally, you can pass a string representation of a sparse vector:

~~~clojure
(pg/execute conn "insert into test3 values ($1, $2)"
            {:params [3 "{1:5.0,2:2.0,3:6.0,5:2.0,6:5.0}/8"]})
~~~

Like the `vector` type, `sparsevec` can be also limited to a certain size:

~~~sql
create table ... (id int, items sparsevec(5))
~~~

The `sparsevec` type supports both binary and text Postgres wire protocol.

## Custom Schemas

The text above assumes you have the `pgvector` extension installed globally
meaning it is hosted in the `public` schema. Sometimes though, extensions are
setup per schema. For example only a schema named `sales` has access to the
`pgvector` extension but nobody else.

If it's your case and you installed `pgvector` into a certain schema, the
standard `:with-pgvector?` flag won't work. By default, PG2 scans the `pg_types`
table for the `public.vector` and `public.sparsevec` types. Since the schema
name is not `public` but `sales`, you need to specify it by passing a special
option called `:type-map`. It's a map where keys are fully qualified type names
(either a keyword or a string), and values are predefined instances of the
`IProcessor` interface:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 5432
   :user "test"
   :password "test"
   :database "test"
   :type-map {"sales.vector" t/vector
              "sales.sparsevec" t/sparsevec}})
~~~

You can rely on keywords as well:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 5432
   :user "test"
   :password "test"
   :database "test"
   :type-map {:sales/vector t/vector
              :sales/sparsevec t/sparsevec}})
~~~

The `t` alias references the `pg.type` namespace.

Now if you install the extension into the `statistics` schema as well, add it
into the map:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 5432
   :user "test"
   :password "test"
   :database "test"
   :type-map {:sales/vector t/vector
              :sales/sparsevec t/sparsevec
              :statistics/vector t/vector
              :statistics/sparsevec t/sparsevec}})
~~~

Should you make a mistake in a fully qualified type name, it will be ignored,
and you'll get value from the database unparsed. The actual value depends on the
binary encoding and decoding options of a connection. By default, it uses text
protocol so you'll get a string like "[1, 2, 3]". For binary encoding and
decoding, you'll get a byte array that holds raw Postgres payload.
