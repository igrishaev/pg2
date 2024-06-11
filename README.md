# PG2: A *Fast* PostgreSQL Driver For Clojure

[pg]: https://github.com/igrishaev/pg

PG2 is a client library for PostgreSQL server. It succeeds [PG(one)][pg] -- my
early attempt to make a JDBC-free client. Comparing to it, PG2 has the following
features:

**It's fast.** Benchmarks prove up to 3 times performance boost compared to
Next.JDBC. A simple HTTP application which reads data from the database and
responds with JSON handles 2 times more RPS. For details, see the "benchmarks"
below.

**It's written in Java** with a Clojure layer on top of it. Unfortunately,
Clojure is not as fast as Java. For performance sake, I've got to implement most
of the logic in pure Java. The Clojure layer is extremely thin and serves only
to call certain methods.

It's still **Clojure friendly**: by default, all the queries return a vector of
maps, where the keys are keywords. You don't need to remap the result in any
way. But moreover, the library provides dozens of ways to group, index, a reduce
a result.

It **supports JSON** out from the box: There is no need to extend any protocols
and so on. Read and write json(b) with ease as Clojure data! Also, JSON reading
and writing as *really fast*, again: 2-3 times faster than in Next.JDBC.

It **supports COPY operations**: you can easily COPY OUT a table into a
stream. You can COPY IN a set of rows without CSV-encoding them because it's
held by the library. It also supports binary COPY format, which is faster.

It **supports java.time.** classes. The ordinary JDBC clients still use
`Timestamp` class for dates, which is horrible. In PG2, all the `java.time.*`
classes are supported for reading and writing.

...And plenty of other features.

## Table of Contents

- [Installation](docs/installation.md)
- [Quick start (Demo)](docs/quick-start.md)
- [Benchmarks](docs/benchmarks.md)
- [Authentication](docs/authentication.md)
- [Connecting to the server](docs/connecting.md)
- [Query and Execute](docs/query-execute.md)
- Common Execute parameters
- Type hints
- [Prepared Statements](docs/prepared-statement.md)
- [Transactions](docs/transaction.md)
- [Connection state](docs/connection-state.md)
- [HoneySQL Integration](docs/honeysql.md)
- [HugSQL Support](docs/hugsql.md)
- [Next.JDBC API layer](#nextjdbc-api-layer)
- [Enums](#enums)
- [Cloning a Connection](#cloning-a-connection)
- [Cancelling a Query](#cancelling-a-query)
- [Thread Safety](#thread-safety)
- [Result reducers](#result-reducers)
- [COPY IN/OUT](#copy-inout)
- [SSL](#ssl)
- [Type Mapping](#type-mapping)
- [JSON support](#json-support)
- [Arrays support](#arrays-support)
- [Notify/Listen (PubSub)](#notifylisten-pubsub)
- [Notices](#notices)
- [Logging](#logging)
- [Errors and Exceptions](#errors-and-exceptions)
- [Connection Pool](#connection-pool)
- [Component integration](#component-integration)
- [Ring middleware](#ring-middleware)
- [Migrations](docs/migrations.md)
- [Debugging](#debugging)
- [Running tests](#running-tests)
- [Running benchmarks](#running-benchmarks)



## Next.JDBC API layer

[next-jdbc]: https://github.com/seancorfield/next-jdbc

PG2 has a namespace that mimics [Next.JDBC][next-jdbc] API. Of course, it
doesn't cover 100% of Next.JDBC features yet most of the functions and macros
are there. It will help you to introduce PG2 into the project without rewriting
all the database-related code from scratch.

### Obtaining a Connection

In Next.JDBC, all the functions and macros accept something that implements the
`Connectable` protocol. It might be a plain Clojure map, an existing connection,
or a connection pool. The PG2 wrapper follows this design. It works with either
a map, a connection, or a pool.

Import the namespace and declare a config:

~~~clojure
(require '[pg.jdbc :as jdbc])

(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"})
~~~

Having a config map, obtain a connection by passing it into the `get-connection`
function:

~~~clojure
(def conn
  (jdbc/get-connection config))
~~~

This approach, although is a part of the Next.JDBC design, is not recommended to
use. Once you've established a connection, you must either close it or, if it
was borrowed from a pool, return it to the pool. There is a special macro
`on-connection` that covers this logic:

~~~clojure
(jdbc/on-connection [bind source]
  ...)
~~~

If the `source` was a map, a new connection is spawned and gets closed
afterwards. If the `source` is a pool, the connection gets returned to the pool.
When the `source` is a connection, nothing happens when exiting the macro.

~~~clojure
(jdbc/on-connection [conn config]
  (println conn))
~~~

A brief example with a connection pool and a couple of futures. Each future
borrows a connection from a pool, and returns it afterwards.

~~~clojure
(pool/with-pool [pool config]
  (let [f1
        (future
          (jdbc/on-connection [conn1 pool]
            (println
             (jdbc/execute-one! conn1 ["select 'hoho' as message"]))))
        f2
        (future
          (jdbc/on-connection [conn2 pool]
            (println
             (jdbc/execute-one! conn2 ["select 'haha' as message"]))))]
    @f1
    @f2))

;; {{:message hoho}:message haha}
;; two overlapping print statements
~~~

### Executing Queries

Two functions `execute!` and `execute-one!` send queries to the database. Each
of them takes a source, a SQL vector, and a map of options. The SQL vector is a
sequence where the first item is either a string or a prepared statement, and
the rest values are parameters.

~~~clojure
(jdbc/on-connection [conn config]
  (jdbc/execute! conn ["select $1 as num" 42]))
;; [{:num 42}]
~~~

Pay attention that parameters use a dollar sign with a number but not a question
mark.

The `execute-one!` function acts like `execute!` but returns the first row
only. Internaly, this is done by passing the `{:first? true}` parameter that
enables the `First` reducer.

~~~clojure
(jdbc/on-connection [conn config]
  (jdbc/execute-one! conn ["select $1 as num" 42]))
;; {:num 42}
~~~

To prepare a statement, pass a SQL-vector into the `prepare` function. The
result will be an instance of the `PreparedStatement` class. To execute a
statement, put it into a SQL-vector followed by the parameters:

~~~clojure
(jdbc/on-connection [conn config]
  (let [stmt
        (jdbc/prepare conn
                      ["select $1::int4 + 1 as num"])
        res1
        (jdbc/execute-one! conn [stmt 1])

        res2
        (jdbc/execute-one! conn [stmt 2])]

    [res1 res2]))

;; [{:num 2} {:num 3}]
~~~

Above, the same `stmt` statement is executed twice with different parameters.

More realistic example with inserting data into a table. Let's prepare the table
first:

~~~clojure
(jdbc/execute! config ["create table test2 (id serial primary key, name text not null)"])
~~~

Insert a couple of rows returning the result:

~~~clojure
(jdbc/on-connection [conn config]
  (let [stmt
        (jdbc/prepare conn
                      ["insert into test2 (name) values ($1) returning *"])

        res1
        (jdbc/execute-one! conn [stmt "Ivan"])

        res2
        (jdbc/execute-one! conn [stmt "Huan"])]

    [res1 res2]))

;; [{:name "Ivan", :id 1} {:name "Huan", :id 2}]
~~~

As it was mentioned above, in Postgres, a prepared statement is always bound to
a certain connection. Thus, use the `prepare` function only inside the
`on-connection` macro to ensure that all the underlying database interaction is
made within the same connection.

### Transactions

The `with-transaction` macro wraps a block of code into a transaction. Before
entering the block, the macro emits the `BEGIN` expression, and `COMMIT`
afterwards, if there was no an exception. Should an exception pop up, the
transaction gets rolled back with `ROLLBACK`, and the exception is re-thrown.

The macro takes a binding symbol which a connection is bound to, a source, an a
map of options. The standard Next.JDBC transaction options are supported,
namely:

- `:isolation`
- `:read-only`
- `:rollback-only`

Here is an example of inserting a couple of rows in a transaction:

~~~clojure
(jdbc/on-connection [conn config]

  (let [stmt
        (jdbc/prepare conn
                      ["insert into test2 (name) values ($1) returning *"])]

    (jdbc/with-transaction [TX conn {:isolation :serializable
                                     :read-only false
                                     :rollback-only false}]

      (let [res1
            (jdbc/execute-one! conn [stmt "Snip"])

            res2
            (jdbc/execute-one! conn [stmt "Snap"])]

        [res1 res2]))))

;; [{:name "Snip", :id 3} {:name "Snap", :id 4}]
~~~

The Postgres log:

~~~
BEGIN
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
insert into test2 (name) values ($1) returning *
  $1 = 'Snip'
insert into test2 (name) values ($1) returning *
  $1 = 'Snap'
COMMIT
~~~

The `:isolation` parameter might be one of the following:

- `:read-uncommitted`
- `:read-committed`
- `:repeatable-read`
- `:serializable`

To know more about transaction isolation, refer to the official [Postgres
documentation][transaction-iso].

When `read-only` is true, any mutable query will trigger an error response from
Postgres:

~~~clojure
(jdbc/with-transaction [TX config {:read-only true}]
  (jdbc/execute! TX ["delete from test2"]))

;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:207).
;; Server error response: {severity=ERROR, message=cannot execute DELETE in a read-only transaction, verbosity=ERROR}
~~~

When `:rollback-only` is true, the transaction gets rolled back even there was
no an exception. This is useful for tests and experiments:

~~~clojure
(jdbc/with-transaction [TX config {:rollback-only true}]
  (jdbc/execute! TX ["delete from test2"]))
~~~

The logs:

~~~
statement: BEGIN
execute s1/p2: delete from test2
statement: ROLLBACK
~~~

The table still has its data:

~~~clojure
(jdbc/execute! config ["select * from test2"])

;; [{:name "Ivan", :id 1} ...]
~~~

The function `active-tx?` helps to determine if you're in the middle of a
transaction:

~~~clojure
(jdbc/on-connection [conn config]
  (let [res1 (jdbc/active-tx? conn)]
    (jdbc/with-transaction [TX conn]
      (let [res2 (jdbc/active-tx? TX)]
        [res1 res2]))))

;; [false true]
~~~

It returns `true` for transactions tha are in the error state as well.

### Keys and Namespaces

The `pg.jdbc` wrapper tries to mimic Next.JDBC and thus uses `kebab-case-keys`
when building maps:

~~~clojure
(jdbc/on-connection [conn config]
  (jdbc/execute-one! conn ["select 42 as the_answer"]))

;; {:the-answer 42}
~~~

To change that behaviour and use `snake_case_keys`, pass the `{:kebab-keys?
false}` option map:

~~~clojure
(jdbc/on-connection [conn config]
  (jdbc/execute-one! conn
                     ["select 42 as the_answer"]
                     {:kebab-keys? false}))

;; {:the_answer 42}
~~~

By default, Next.JDBC returns full-qualified keys where namespaces are table
names, for example `:user/profile-id` or `:order/created-at`. At the moment,
namespaces are not supported by the wrapper.

## Enums

## Cloning a Connection

## Cancelling a Query

## Thread Safety

## Result reducers

## COPY IN/OUT

## SSL

## Type Mapping

## JSON support

Postgres is amazing when dealing with JSON. There hardly can be a database that
serves it better. Unfortunately, Postgres clients never respect the JSON
feature, which is horrible. Take JDBC, for example: when querying a JSON(b)
value, you'll get a dull `PGObject` which should be decoded manually. The same
applies to insertion: one cannot just pass a Clojure map or a vector. It should
be packed into the `PGObject` as well.

Of course, this can be automated by extending certain protocols. But it's still
slow as it's done on Clojure level (not Java), and it forces you to copy the
same code across projects.

Fortunately, PG2 supports JSON out from the box. If you query a JSON value,
you'll get its Clojure counter-part: a map, a vector, etc. To insert a JSON
value to a table, you pass either a Clojure map or a vector. No additional steps
are required.

[jsonista]: https://github.com/metosin/jsonista

PG2 relies on [jsonista][jsonista] library to handle JSON. At the moment of
writing, this is the fastest JSON library for Clojure. Jsonista uses a concept
of object mappers: objects holding custom rules to encode and decode values. You
can compose your own mapper with custom rules and pass it into the connection
config.

### Basic usage

Let's prepare a connection and a test table with a jsonb column:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"})

(def conn
  (jdbc/get-connection config))

(pg/query conn "create table test_json (
  id serial primary key,
  data jsonb not null
)")
~~~

Now insert a row:

~~~clojure
(pg/execute conn
            "insert into test_json (data) values ($1)"
            {:params [{:some {:nested {:json 42}}}]})
~~~

No need to encode a map manually nor wrap it into a sort of `PGObject`. Let's
fetch the new row by id:

~~~clojure
(pg/execute conn
            "select * from test_json where id = $1"
            {:params [1]
             :first? true})

{:id 1 :data {:some {:nested {:json 42}}}}
~~~

Again, the JSON data returns as a Clojure map with no wrappers.

When using JSON with HoneySQL though, some circs are still needed. Namely, you
have to wrap a value with `[:lift ...]` as follows:

~~~clojure
(pgh/insert-one conn
                :test_json
                {:data [:lift {:another {:json {:value [1 2 3]}}}]})

{:id 2, :data {:another {:json {:value [1 2 3]}}}}
~~~

Without the `[:lift ...]` tag, HoneySQL will treat the value as a nested SQL map
and try to render it as a string, which will fail of course or lead to a SQL
injection.

Another way is to use HoneySQL parameters conception:

~~~clojure
(pgh/insert-one conn
                :test_json
                {:data [:param :data]}
                {:honey {:params {:data {:some [:json {:map [1 2 3]}]}}}})
~~~

For details, see the [HoneySQL Integration](docs/honeysql.md) section.

PG2 supports only Clojure maps when encoding values into JSON. Vectors and other
sequential values are treated as arrays. For details, see the [Arrays
support](#arrays-support) section.

### Json Wrapper

In rare cases you might store a string or a number in a JSON field. Say, 123 is
a valid JSON value but it's treated as a number. To tell Postgres it's a JSON
indeed, wrap the value with `pg/json-wrap`:

~~~clojure
(pgh/insert-one conn
                :test_json
                {:data (pg/json-wrap 42)})

{:id 4, :data 42}
~~~

The wrapper is especially useful to store a "null" JSON value: not the standard
`NULL` but `"null"` which, when parsed, becomes `nil`. For this, pass
`(pg/json-wrap nil)` as follows:

~~~clojure
(pgh/insert-one conn
                :test_json
                {:data (pg/json-wrap nil)})

{:id 5, :data nil} ;; "null" in the database
~~~

### Custom Object Mapper

One great thing about Jsonista is a conception of mapper objects. A mapper is a
set of rules how to encode and decode data. Jsonista provides a way to build a
custom mapper. Once built, it can be passed to a connection config so the JSON
data is written and read back in a special way.

Let's assume you're going to tag JSON sub-parts to track their types. For
example, if encoding a keyword `:foo`, you'll get a vector of `["!kw",
"foo"]`. When decoding that vector, by the `"!kw"` string, the mapper
understands it a keyword and coerces `"foo"` to `:foo`.

Here is how you create a mapper with Jsonista:

~~~clojure

(ns ...
  (:import
   clojure.lang.Keyword
   clojure.lang.PersistentHashSet)
  (:require
    [jsonista.core :as j]
    [jsonista.tagged :as jt]))

(def tagged-mapper
  (j/object-mapper
   {:encode-key-fn true
    :decode-key-fn true
    :modules
    [(jt/module
      {:handlers
       {Keyword {:tag "!kw"
                 :encode jt/encode-keyword
                 :decode keyword}
        PersistentHashSet {:tag "!set"
                           :encode jt/encode-collection
                           :decode set}}})]}))
~~~

The `object-mapper` function accepts even more options but we skip them for now.

Now that you have a mapper, pass it into a config:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"
   :object-mapper tagged-mapper})

(def conn
  (jdbc/get-connection config))
~~~

All the JSON operations made by this connection will use the passed object
mapper. Let's insert a set of keywords:

~~~clojure
(pg/execute conn
            "insert into test_json (data) values ($1)"
            {:params [{:object #{:foo :bar :baz}}]})
~~~

When read back, the JSON value is not a vector of strings any longer but a set
of keywords:

~~~clojure
(pg/execute conn "select * from test_json")

[{:id 1, :data {:object #{:baz :bar :foo}}}]
~~~

To peek a raw JSON value, select it as a plain text and print (just to avoid
escaping quotes):

~~~clojure
(printl (pg/execute conn "select data::text json_raw from test_json where id = 10"))

;; [{:json_raw {"object": ["!set", [["!kw", "baz"], ["!kw", "bar"], ["!kw", "foo"]]]}}]
~~~

If you read that row using another connection with a default object mapper, the
data is returned without expanding tags.

### Utility pg.json namespace

PG2 provides an utility namespace for JSON encoding and decoding. You can use it
for files, HTTP API, etc. If you already have PG2 in the project, there is no
need to plug in Cheshire or another JSON library. The namespace is `pg.json`:

~~~clojure
(ns ...
  (:require
   [pg.json :as json]))
~~~

#### Reading JSON

The `read-string` function reads a value from a JSON string:

~~~clojure
(json/read-string "[1, 2, 3]")

[1 2 3]
~~~

The first argument might be an object mapper:

~~~clojure
(json/read-string tagged-mapper "[\"!kw\", \"hello\"]")

:hello
~~~

The functions `read-stream` and `read-reader` act the same but accept either an
`InputStream` or a `Reader` object:

~~~clojure
(let [in (-> "[1, 2, 3]" .getBytes io/input-stream)]
  (json/read-stream tagged-mapper in))

(let [in (-> "[1, 2, 3]" .getBytes io/reader)]
  (json/read-reader tagged-mapper in))
~~~

#### Writing JSON

The `write-string` function dumps an value into a JSON string:

~~~clojure
(json/write-string {:test [:hello 1 true]})

;; "{\"test\":[\"hello\",1,true]}"
~~~

The first argument might be a custom object mapper. Let's reuse our tagger
mapper:

~~~clojure
(json/write-string tagged-mapper {:test [:hello 1 true]})

;; "{\"test\":[[\"!kw\",\"hello\"],1,true]}"
~~~

The functions `write-stream` and `write-writer` act the same. The only
difference is, they accept either an `OutputStream` or `Writer` objects. The
first argument might be a mapper as well:

~~~clojure
(let [out (new ByteArrayOutputStream)]
  (json/write-stream tagged-mapper {:foo [:a :b :c]} out))

(let [out (new StringWriter)]
  (json/write-writer tagged-mapper {:foo [:a :b :c]} out))
~~~

### Ring HTTP middleware

[ring-json]: https://github.com/ring-clojure/ring-json

PG2 provides an HTTP Ring middleware for JSON. It acts like `wrap-json-request`
and `wrap-json-response` middleware from the [ring-json][ring-json]
library. Comparing to it, the PG2 stuff has the following advantages:

- it's faster because of Jsonista, whereas Ring-json relies on Cheshire;
- it wraps both request and response at once with a shortcut;
- it supports custom object mappers.

Imagine you have a Ring handler that reads JSON body and returns a JSON
map. Something like this:

~~~clojure
(defn api-handler [request]
  (let [user-id (-> request :data :user_id)
        user (get-user-by-id user-id)]
    {:status 200
     :body {:user user}}))
~~~

Here is how you wrap it:

~~~clojure
(ns ...
  (:require
   [pg.ring.json :refer [wrap-json
                         wrap-json-response
                         wrap-json-request]]))

(def app
  (-> api-handler
      (wrap-this-foo)
      (wrap-json <opt>)
      (wrap-that-bar)))
~~~

Above, the `wrap-json` wrapper is a combination of `wrap-json-request` and
`wrap-json-response`. You can apply them both explicitly:

~~~clojure
(def app
  (-> api-handler
      (wrap-this-foo)
      (wrap-json-request <opt>)
      (wrap-json-response <opt>)
      (wrap-that-bar)))
~~~

All the three `wrap-json...` middleware accept a handler to wrap and a map of
options. Here is the options supported:

| Name                  | Direction         | Description                                                |
|-----------------------|-------------------|------------------------------------------------------------|
| `:object-mapper`      | request, response | An custom instance of `ObjectMapper`                       |
| `:slot`               | request           | A field to `assoc` the parsed JSON data (1)                |
| `:malformed-response` | request           | A ring response returned when payload cannot be parsed (2) |

Notes:

1. The default slot name is `:json`. Please avoid using `:body` or `:params` to
   prevent overriding existing request fields. This is especially important for
   `:body`! Often, you need the origin input stream to calculate an MD5 or
   SHA-256 hash-sum of the payload. If you overwrite the `:body` field, you
   cannot do that.

2. The default malformed response is something like 400 "Malformed JSON" (plain
   text).

A full example:

~~~clojure
(def json-opt
  {:slot :data
   :object-mapper tagged-mapper ;; see above
   :malformed-response {:status 404
                        :body "<h1>Bad JSON</h1>"
                        :headers {"content-type" "text/html"}}})

(def app
  (-> api-handler
      (wrap-this-foo)
      (wrap-json json-opt)
      (wrap-that-bar)))
~~~~~~

## Arrays support

In JDBC, arrays have always been a pain. Every time you're about to pass an
array to the database and read it back, you've got to wrap your data in various
Java classes, extend protocols, and multimethods. In Postgres, the array type is
quite powerful yet underestimated due to poor support of drivers. This is one
more reason for running this project: to bring easy access to Postgres arrays.

PG2 tries its best to provide seamless connection between Clojure vectors and
Postgres arrays. When reading an array, you get a Clojure vector. And vice
versa: to pass an array object into a query, just submit a vector.

PG2 supports arrays of any type: not only primitives like numbers and strings
but `uuid`, `numeric`, `timestamp(tz)`, `json(b)`, and more as well.

Arrays might have more than one dimension. Nothing prevents you from having a 3D
array of integers like `cube::int[][][]`, and it becomes a nested vector when
fetched by PG2.

*A technical note: PG2 supports both encoding and decoding of arrays in both
text and binary modes.*

Here is a short demo session. Let's prepare a table with an array of strings:

~~~clojure
(pg/query conn "create table arr_demo_1 (id serial, text_arr text[])")
~~~

Insert a simple item:

~~~clojure
(pg/execute conn
            "insert into arr_demo_1 (text_arr) values ($1)"
            {:params [["one" "two" "three"]]})
~~~

In arrays, some elements might be NULL:

~~~clojure
(pg/execute conn
            "insert into arr_demo_1 (text_arr) values ($1)"
            {:params [["foo" nil "bar"]]})
~~~

Now let's check what we've got so far:

~~~clojure
(pg/query conn "select * from arr_demo_1")

[{:id 1 :text_arr ["one" "two" "three"]}
 {:id 2 :text_arr ["foo" nil "bar"]}]
~~~

Postgres supports plenty of operators for arrays. Say, the `&&` one checks if
there is at least one common element on both sides. Here is how we find those
records that have either "tree", "four", or "five":

~~~clojure
(pg/execute conn
            "select * from arr_demo_1 where text_arr && $1"
            {:params [["three" "four" "five"]]})

[{:text_arr ["one" "two" "three"], :id 1}]
~~~

Another useful operator is `@>` that checks if the left array contains all
elements from the right array:

~~~clojure
(pg/execute conn
            "select * from arr_demo_1 where text_arr @> $1"
            {:params [["foo" "bar"]]})

[{:text_arr ["foo" nil "bar"], :id 2}]
~~~

Let's proceed with numeric two-dimensional arrays. They're widely used in math,
statistics, graphics, and similar areas:

~~~clojure
(pg/query conn "create table arr_demo_2 (id serial, matrix bigint[][])")
~~~

Here is how you insert a matrix:

~~~clojure
(pg/execute conn
            "insert into arr_demo_2 (matrix) values ($1)"
            {:params [[[[1 2] [3 4] [5 6]]
                       [[6 5] [4 3] [2 1]]]]})

{:inserted 1}
~~~

Pay attention: each number can be NULL but you cannot have NULL for an entire
sub-array. This will trigger an error response from Postgres.

Reading the matrix back:

~~~clojure
(pg/query conn "select * from arr_demo_2")

[{:id 1 :matrix [[[1 2] [3 4] [5 6]]
                 [[6 5] [4 3] [2 1]]]}]
~~~

A crazy example: let's have a three dimension array of timestamps with a time
zone. No idea how it can be used but still:

~~~clojure
(pg/query conn "create table arr_demo_3 (id serial, matrix timestamp[][][])")

(def -matrix
  [[[[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]]
   [[[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]]
   [[[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]]])

(pg/execute conn
            "insert into arr_demo_3 (matrix) values ($1)"
            {:params [-matrix]})
~~~

Now read it back:

~~~clojure
(pg/query conn "select * from arr_demo_3")

[{:matrix
  [... truncated
   [[[#object[java.time.LocalDateTime 0x5ed6e62b "2024-04-01T18:32:48.272169"]
      #object[java.time.LocalDateTime 0xb9d6851 "2024-04-01T18:32:48.272197"]
      #object[java.time.LocalDateTime 0x6e35ed84 "2024-04-01T18:32:48.272207"]]
     ...
     [#object[java.time.LocalDateTime 0x7319d217 "2024-04-01T18:32:48.272236"]
      #object[java.time.LocalDateTime 0x6153154d "2024-04-01T18:32:48.272241"]
      #object[java.time.LocalDateTime 0x2e4ffd44 "2024-04-01T18:32:48.272247"]]]
    ...
    [[#object[java.time.LocalDateTime 0x32c6e526 "2024-04-01T18:32:48.272405"]
      #object[java.time.LocalDateTime 0x496a5bc6 "2024-04-01T18:32:48.272418"]
      #object[java.time.LocalDateTime 0x283531ee "2024-04-01T18:32:48.272426"]]
     ...
     [#object[java.time.LocalDateTime 0x677b3def "2024-04-01T18:32:48.272459"]
      #object[java.time.LocalDateTime 0x46d5039f "2024-04-01T18:32:48.272467"]
      #object[java.time.LocalDateTime 0x3d0b906 "2024-04-01T18:32:48.272475"]]]]],
  :id 1}]
~~~

You can have an array of JSON(b) objects, too:

~~~clojure
(pg/query conn "create table arr_demo_4 (id serial, json_arr jsonb[])")
~~~

Inserting an array of three maps:

~~~clojure
  (pg/execute conn
              "insert into arr_demo_4 (json_arr) values ($1)"
              {:params [[{:foo 1} {:bar 2} {:test [1 2 3]}]]})
~~~

Elements might be everything that can be JSON-encoded: numbers, strings,
boolean, etc. The only tricky case is a vector. To not break the algorithm that
traverses the matrix, wrap a vector element with `pg/json-wrap`:

~~~clojure
(pg/execute conn
            "insert into arr_demo_4 (json_arr) values ($1)"
            {:params [[42 nil {:some "object"} (pg/json-wrap [1 2 3])]]})

;; Signals that the [1 2 3] is not a nested array but an element.
~~~

Now read it back:

~~~clojure
(pg/query conn "select * from arr_demo_4")

[{:id 1, :json_arr [42 nil {:some "object"} [1 2 3]]}]
~~~

## Notify/Listen (PubSub)

## Notices

## Logging

## Errors and Exceptions

## Connection Pool

## Component integration

## Ring middleware

<!-- migrations -->

## Debugging

## Running tests

## Running benchmarks
