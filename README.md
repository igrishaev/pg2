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
`Timestamp` class for dates, which is horrible. In PG2, all the java.time.*
classes are supported for reading and writing.

...And plenty of other features.

## Table of Contents

<!-- toc -->

- [Installation](#installation)
- [Quick start (Demo)](#quick-start-demo)
- [Benchmarks](#benchmarks)
- [Authentication](#authentication)
- [Connecting to the server](#connecting-to-the-server)
- [Connection config parameters](#connection-config-parameters)
- [Query](#query)
- [Execute](#execute)
- [Common Execute parameters](#common-execute-parameters)
- [Type hints](#type-hints)
- [Prepared Statements](#prepared-statements)
- [Transactions](#transactions)
- [Connection state](#connection-state)
- [HoneySQL Integration & Shortcuts](#honeysql-integration--shortcuts)
  * [Get by id](#get-by-id)
  * [Get by ids](#get-by-ids)
  * [Delete](#delete)
  * [Insert](#insert)
- [Next.JDBC API layer](#nextjdbc-api-layer)
  * [Obtaining a Connection](#obtaining-a-connection)
  * [Executing Queries](#executing-queries)
  * [Transactions](#transactions-1)
  * [Keys and Namespaces](#keys-and-namespaces)
- [Enums](#enums)
- [Cloning a Connectin](#cloning-a-connectin)
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
- [Debugging](#debugging)
- [Running tests](#running-tests)
- [Running benchmarks](#running-benchmarks)

<!-- tocstop -->

## Installation

**Core functionality**: the client and the connection pool, type encoding and
decoding, COPY IN/OUT, SSL:

~~~clojure
;; lein
[com.github.igrishaev/pg2-core "0.1.3"]

;; deps
com.github.igrishaev/pg2-core {:mvn/version "0.1.3"}
~~~

**HoneySQL integration**: special version of `query` and `execute` that accept
not a string of SQL but a map that gets formatted to SQL under the hood. Also
includes various helpers (`get-by-id`, `find`, `insert`, `udpate`, `delete`,
etc).

~~~clojure
;; lein
[com.github.igrishaev/pg2-honey "0.1.3"]

;; deps
com.github.igrishaev/pg2-honey {:mvn/version "0.1.3"}
~~~

[component]: https://github.com/stuartsierra/component

**Component integration**: a package that extends the `Connection` and `Pool`
objects with the `Lifecycle` protocol from the [Component][component] library.

~~~clojure
;; lein
[com.github.igrishaev/pg2-component "0.1.3"]

;; deps
com.github.igrishaev/pg2-component {:mvn/version "0.1.3"}
~~~

## Quick start (Demo)

This is a very brief passage through the library:

~~~clojure
(ns pg.demo
  (:require
   [pg.core :as pg]))

;; declare a minimal config
(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :database "test"})


;; connect to the database
(def conn
  (pg/connect config))


;; a trivial query
(pg/query conn "select 1 as one")
;; [{:one 1}]


;; let's create a table
(pg/query conn "
create table demo (
  id serial primary key,
  title text not null,
  created_at timestamp with time zone default now()
)")
;; {:command "CREATE TABLE"}


;; Insert three rows returning all the columns.
;; Pay attention that PG2 uses not question marks (?)
;; but numered dollars for parameters:
(pg/execute conn
            "insert into demo (title) values ($1), ($2), ($3)
             returning *"
            {:params ["test1" "test2" "test3"]})


;; The result: pay attention we got the j.t.OffsetDateTime class
;; for the timestamptz column (truncated):

[{:title "test1",
  :id 4,
  :created_at #object[j.t.OffsetDateTime "2024-01-17T21:57:58..."]}
 {:title "test2",
  :id 5,
  :created_at #object[j.t.OffsetDateTime "2024-01-17T21:57:58..."]}
 {:title "test3",
  :id 6,
  :created_at #object[j.t.OffsetDateTime "2024-01-17T21:57:58..."]}]


;; Try two expressions in a single transaction
(pg/with-tx [conn]
  (pg/execute conn
              "delete from demo where id = $1"
              {:params [3]})
  (pg/execute conn
              "insert into demo (title) values ($1)"
              {:params ["test4"]}))
;; {:inserted 1}


;; Now check out the database log:

;; LOG:  statement: BEGIN
;; LOG:  execute s3/p4: delete from demo where id = $1
;; DETAIL:  parameters: $1 = '3'
;; LOG:  execute s5/p6: insert into demo (title) values ($1)
;; DETAIL:  parameters: $1 = 'test4'
;; LOG:  statement: COMMIT

~~~

## Benchmarks

[bench1]: https://grishaev.me/en/pg2-bench-1
[bench2]: https://grishaev.me/en/pg2-bench-2
[bench3]: https://grishaev.me/en/pg2-bench-3

See the following posts in my blog:

- [PG2 early announce and benchmarks, part 1][bench1]
- [PG2 benchmarks, part 2][bench2]
- [PG2 release 0.1.2: more performance, benchmarks, part 3][bench3]

## Authentication

The library suppors the following authentication types and pipelines:

- No password (for trusted clients);

- Clear text password (not used nowadays);

- MD5 password with hash (default prior to Postgres ver. 15);

- SASL with the SCRAM-SHA-256 algorithm (default since Postgres ver. 15). The
  SCRAM-SHA-256-PLUS algorithm is not implemented.

## Connecting to the server

To connect the server, define a config map and pass it into the `connect`
function:

~~~clojure
(require '[pg.core :as pg])

(def config
  {:host "127.0.0.1"
   :port 5432
   :user "test"
   :password "test"
   :database "test"})

(def conn
  (pg/connect config))
~~~

The `conn` is an instance of the `org.pg.Connection` class.

The `:host`, `:port`, and `:password` config fields have default values and
might be skipped (the password is an empty string by default). Only the `:user`
and `:database` fields are required when connecting. See the list of possible
fields and their values in a separate section.

To close a connection, pass it into the `close` function:

~~~clojure
(pg/close conn)
~~~

You cannot open or use this connection again afterwards.

To close the connection automatically, use either `with-connection` or
`with-open` macro. The `with-connection` macro takes a binding symbol and a
config map; the connection gets bound to the binding symbold while the body is
executed:

~~~clojure
(pg/with-connection [conn config]
  (pg/query conn "select 1 as one"))
~~~

The standard `with-open` macro calls the `(.close connection)` method on exit:

~~~clojure
(with-open [conn (pg/connect config)]
  (pg/query conn "select 1 as one"))
~~~

Avoid situations when you close a connection manually. Use one of these two
macros shown above.

Use `:pg-params` field to specify connection-specific Postgres parameters. These
are "TimeZone", "application_name", "DateStyle" and more. Both keys and values
are plain strings:

~~~clojure
(def config+
  (assoc config
         :pg-params
         {"application_name" "Clojure"
          "DateStyle" "ISO, MDY"}))

(def conn
  (pg/connect config+))
~~~

## Connection config parameters

The following table describes all the possible connection options with the
possible values and semantics. Only the two first options are requred. All the
rest have predefined values.

| Field                  | Type       | Default      | Comment                                                   |
|------------------------|------------|--------------|-----------------------------------------------------------|
| `:user`                | string     | **required** | the name of the DB user                                   |
| `:database`            | string     | **required** | the name of the database                                  |
| `:host`                | string     | 127.0.0.1    | IP or hostname                                            |
| `:port`                | integer    | 5432         | port number                                               |
| `:password`            | string     | ""           | DB user password                                          |
| `:pg-params`           | map        | {}           | A map of session params like {string string}              |
| `:binary-encode?`      | bool       | false        | Whether to use binary data encoding                       |
| `:binary-decode?`      | bool       | false        | Whether to use binary data decoding                       |
| `:in-stream-buf-size`  | integer    | 0xFFFF       | Size of the input buffered socket stream                  |
| `:out-stream-buf-size` | integer    | 0xFFFF       | Size of the output buffered socket stream                 |
| `:fn-notification`     | 1-arg fn   | logging fn   | A function to handle notifications                        |
| `:fn-protocol-version` | 1-arg fn   | logging fn   | A function to handle negotiation version protocol event   |
| `:fn-notice`           | 1-arg fn   | logging fn   | A function to handle notices                              |
| `:use-ssl?`            | bool       | false        | Whether to use SSL connection                             |
| `:ssl-context`         | SSLContext | nil          | An custom instance of `SSLContext` class to wrap a socket |
| `:so-keep-alive?`      | bool       | true         | Socket KeepAlive value                                    |
| `:so-tcp-no-delay?`    | bool       | true         | Socket TcpNoDelay value                                   |
| `:so-timeout`          | integer    | 15.000       | Socket timeout value, in ms                               |
| `:so-recv-buf-size`    | integer    | 0xFFFF       | Socket receive buffer size                                |
| `:so-send-buf-size`    | integer    | 0xFFFF       | Socket send buffer size                                   |
| `:log-level`           | keyword    | :info        | Connection logging level. See possible values below       |
| `:cancel-timeout-ms`   | integer    | 5.000        | Default value for the `with-timeout` macro, in ms         |
| `:protocol-version`    | integ      | 196608       | Postgres protocol version                                 |

Possible `:log-level` values are:

- `:all` to render all the events
- `:trace`
- `:debug`
- `:info`
- `:error`
- `:off`, `false`, or `nil` to disable logging.

## Query

The `query` function sends a query to the server and returns the
result. Non-data queries return a map with a tag:

~~~clojure
(pg/query conn "create table test1 (id serial primary key, name text)")
{:command "CREATE TABLE"}

(pg/query conn "insert into test1 (name) values ('Ivan'), ('Huan')")
{:inserted 2}
~~~

Data queries return a vector of maps. This behaviour may be changed with
reducers (see below).

~~~clojure
(pg/query conn "select * from test1")
[{:name "Ivan", :id 1} {:name "Huan", :id 2}]
~~~

The SQL string might include several expressions concatenated with a
semicolon. In this case, the result will be a vector of results:

~~~clojure
(pg/query conn "insert into test1 (name) values ('Juan'); select * from test1")

[{:inserted 1}
 [{:name "Ivan", :id 1}
  {:name "Huan", :id 2}
  {:name "Juan", :id 3}]]
~~~

Use this feature wisely; don't try to do lots of things at once.

**Important:** the `query` function doesn't support parameters. You cannot run a
query like these two below or similar:

~~~clojure
(pg/query conn "select * from test1 where id = ?" 1)
;; or
(pg/query conn ["select * from test1 where id = ?" 1])
~~~

**NEVER(!), NEVER(!!), NEVER(!!!) put parameters into a SQL string using `str`,
`format`, or other functions that operate on strings. You will regret it one
day. Use execute with parameters instead.**

## Execute

The `execute` function acts like `query` but has the following peculiarities:

- The SQL string cannot have many expressions concatenated with a
  semicolon. There must be a single expression (although the trailing semicolon
  is allowed).

- It may have parameters. The values for these parameters are passed
  separately. Unlike in JDBC, the parameters use dollar sign with a number, for
  example `$1`, `$2`, etc.

Here is how you can query a row by its primary key:

~~~clojure
(pg/execute conn "select * from test1 where id = $1" {:params [2]})
;; [{:name "Huan", :id 2}]
~~~

The values are passed into the `:params` key; they must be a vector, or a list,
or a lazy sequence. Passing a set is not recommended as it doesn't guarantee the
order of the values.

This is how you insert values into a table using parameters:

~~~clojure
(pg/execute conn
            "insert into test1 (name) values ($1), ($2), ($3)"
            {:params ["Huey" "Dewey" "Louie"]})
;; {:inserted 3}
~~~

Pay attention that the values are always a flat list. Imagine you'd like to
insert rows with explicit ids:

~~~clojure
(pg/execute conn
            "insert into test1 (id, name) values ($1, $2), ($3, $4), ($5, $6)"
            {:params [1001 "Harry" 1002 "Hermione" 1003 "Ron"]})
;; {:inserted 3}
~~~

The `:params` vector consists from flat values but not pairs like `[1001
"Harry"]`. For better readability, make a list of pairs and then `flatten` it:

~~~clojure
(def pairs
  [[1001 "Harry"]
   [1002 "Hermione"]
   [1003 "Ron"]])

(flatten pairs)

;; (1001 "Harry" 1002 "Hermione" 1003 "Ron")
~~~

Since the parameters have explicit numbers, you can reference a certain value
many times. The following query will create three agents Smith with different
ids.

~~~clojure
(pg/execute conn
            "insert into test1 (name) values ($1), ($1), ($1)"
            {:params ["Agent Smith"]})
;; {:inserted 3}
~~~

Both `query` and `execute` functions accept various options that affect data
processing. Find their description in the next section.

## Common Execute parameters

## Type hints

## Prepared Statements

In Postgres, prepared statements are queries that have passed all the
preliminary stages and now ready to be executed. Running the same prepared
statement with different parameters is faster than executing a fresh query each
time. To prepare a statement, pass a SQL expression into the `prepare`
function. It will return a special `PreparedStatement` object:

~~~clojure
(def stmt-by-id
  (pg/prepare conn "select * from test1 where id = $1"))

(str stmt-by-id)
<Prepared statement, name: s11, param(s): 1, OIDs: [INT4], SQL: select * from test1 where id = $1>
~~~

The statement might have parameters. Now that you have a statement, execute it
with the `execute-statement` function. Below, we execute it three times with
various primary keys. We also pass the `:first?` option set to true to have only
row in the result.

~~~clojure
(pg/execute-statement conn
                      stmt-by-id
                      {:params [1] :first? true})
;; {:name "Ivan", :id 1}

(pg/execute-statement conn
                      stmt-by-id
                      {:params [5] :first? true})
;; {:name "Louie", :id 5}

(pg/execute-statement conn
                      stmt-by-id
                      {:params [8] :first? true})
;; {:name "Agent Smith", :id 8}
~~~

During its lifetime on the server, a statement consumes some resources. When
it's not needed any longer, release it with the `close-statement` function:

~~~clojure
(pg/close-statement conn stmt-by-id)
~~~

The following macro helps to auto-close a statement. The first argument is a
binding symbol. It will be pointing to a fresh prepared statement during the
execution of the body. Afterwards, the statement is closed.

Below, we insert tree rows in the database using the same prepared
statement. Pay attention to the `doall` clause: it evaluates the lazy sequence
produced by `for`. Without `doall`, you'll get an error from the server saying
there is no such a prepared statement.

~~~clojure
(pg/with-statement [stmt conn "insert into test1 (name) values ($1) returning *"]
  (doall
   (for [character ["Agent Brown"
                    "Agent Smith"
                    "Agent Jones"]]
     (pg/execute-statement conn stmt {:params [character] :first? true}))))

({:name "Agent Brown", :id 12}
 {:name "Agent Smith", :id 13}
 {:name "Agent Jones", :id 14})
~~~

**In Postgres, prepared statements are always bound to a certain
connection. Don't share a statement opened in a connection A to B and vice
versa. Do not share them across different threads.**

## Transactions

To execute one or more queries in a transaction, wrap them with `begin` and
`commit` functions as follows:

~~~clojure
(pg/begin conn)

(pg/execute conn
            "insert into test1 (name) values ($1)"
            {:params ["Test1"]})

(pg/execute conn
            "insert into test1 (name) values ($1)"
            {:params ["Test2"]})

(pg/commit conn)
~~~

Both rows are inserted in a transaction. Should one of them fail, none will
succeed. By checking the database log, you'll see the following entries:

~~~
statement: BEGIN
execute s23/p24: insert into test1 (name) values ($1)
  parameters: $1 = 'Test1'
execute s25/p26: insert into test1 (name) values ($1)
  parameters: $1 = 'Test2'
statement: COMMIT
~~~

The `rollback` function rolls back the current transaction. The "Test3" entry
will be available during transaction but won't be stored at the end.

~~~clojure
(pg/begin conn)
(pg/execute conn
            "insert into test1 (name) values ($1)"
            {:params ["Test3"]})
(pg/rollback conn)
~~~

Of course, there is a macro what handles `BEGIN`, `COMMIT`, and `ROLLBACK` logic
for you. The `with-tx` one wraps a block of code. It opens a transaction,
executes the body and, if there was not an exception, commits it. If there was
an exception, the macro rolls back the transaction and re-throws it.

The first argument of the macro is a connection object:

~~~clojure
(pg/with-tx [conn]
  (pg/execute conn
              "delete from test1 where name like $1"
              {:params ["Test%"]})
  (pg/execute conn
              "insert into test1 (name) values ($1)"
              {:params ["Test3"]}))
~~~

The macro expands into something like this:

~~~clojure
(pg/begin conn)
(try
  (let [result (do <body>)]
    (pg/commit conn)
    result)
  (catch Throwable e
    (pg/rollback conn)
    (throw e)))
~~~

The macro accepts several optional parameters that affect a transaction, namely:

| Name               | Type              | Description                                                                  |
|--------------------|-------------------|------------------------------------------------------------------------------|
| `:read-only?`      | Boolean           | When true, only read operations are allowed                                  |
| `:rollback?`       | Boolean           | When true, he transaction gets rolled back even if there was no an exception |
| `:isolation-level` | Keyword or String | Set isolation level for the current transaction                              |

The `:read-only?` parameter set to true restricts all the queries in this
transaction to be read only. Thus, only `SELECT` queries will work. Running
`INSERT`, `UPDATE`, or `DELETE` will cause an exception:

~~~clojure
(pg/with-tx [conn {:read-only? true}]
  (pg/execute conn
              "delete from test1 where name like $1"
              {:params ["Test%"]}))

;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:205).
;; Server error response: {severity=ERROR, code=25006, ... message=cannot execute DELETE in a read-only transaction, verbosity=ERROR}
~~~

The `:rollback?` parameter, when set to true, rolls back a transaction even if
it was successful. This is useful for tests:

~~~clojure
(pg/with-tx [conn {:rollback? true}]
  (pg/execute conn
              "delete from test1 where name like $1"
              {:params ["Test%"]}))

;; statement: BEGIN
;; execute s11/p12: delete from test1 where name like $1
;;   parameters: $1 = 'Test%'
;; statement: ROLLBACK
~~~

Above, inside the `with-tx` macro, you'll have all the rows deleted but once you
get back, they will be there again.

Finally, the `:isolation-level` parameter sets isolation level for the current
transaction. The table below shows its possible values:

| `:isolation-level` parameter              | Postgres level   |
|-------------------------------------------|------------------|
| `:read-uncommitted`, `"READ UNCOMMITTED"` | READ UNCOMMITTED |
| `:read-committed`, `"READ COMMITTED"`     | READ COMMITTED   |
| `:repeatable-read`, `"REPEATABLE READ"`   | REPEATABLE READ  |
| `:serializable`, `"SERIALIZABLE"`         | SERIALIZABLE     |

Usage:

~~~clojure
(pg/with-tx [conn {:isolation-level :serializable}]
  (pg/execute conn
              "delete from test1 where name like $1"
              {:params ["Test%"]})
  (pg/execute conn
              "insert into test1 (name) values ($1)"
              {:params ["Test3"]}))

;; statement: BEGIN
;; statement: SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
;; execute s33/p34: delete from test1 where name like $1
;;   parameters: $1 = 'Test%'
;; execute s35/p36: insert into test1 (name) values ($1)
;;   parameters: $1 = 'Test3'
;; statement: COMMIT
~~~

The default transation level depends on the settings of your database.

[transaction-iso]: https://www.postgresql.org/docs/current/transaction-iso.html

This document doesn't describe the difference between isolation levels. Please
refer to the [official documentation][transaction-iso] for more information.

## Connection state

There are some function to track the connection state. In Postgres, the state of
a connection might be one of these:

- **idle** when it's ready for a query;

- **in transaction** when a transaction has already been started but not
  committed yet;

- **error** when transaction has failed but hasn't been rolled back yet.

The `status` function returns either `:I`, `:T`, or `:E` keywords depending on
where you are at the moment. For each state, there is a corresponding predicate
that returns either true of false.

At the beginning, the connection is idle:

~~~clojure
(pg/status conn)
:I

(pg/idle? conn)
true
~~~

Open a transaction and check out the state:

~~~clojure
(pg/begin conn)

(pg/status conn)
:T

(pg/in-transaction? conn)
true
~~~

Now send a broken query to the server. You'll get an exception describing the
error occurred on the server:

~~~clojure
(pg/query conn "selekt dunno")

;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:205).
;; Server error response: {severity=ERROR, code=42601, ...,  message=syntax error at or near "selekt", verbosity=ERROR}
~~~

The connection is in the error state now:

~~~clojure
(pg/status conn)
:E

(pg/tx-error? conn)
true
~~~

When state is error, the connection doesn't accept any new queries. Each of them
will be rejected with a message saying that the connection is in the error
state. To recover from an error, rollback the current transaction:

~~~clojure
(pg/rollback conn)

(pg/idle? conn)
true
~~~

Now it's ready for new queries again.

## HoneySQL Integration & Shortcuts

[honeysql]: https://github.com/seancorfield/honeysql

The `pg-honey` package allows you to call `query` and `execute` functions
passing not string SQL expressions but Clojure maps. Internally, they get
transformed into SQL using the great [HoneySQL library][honeysql]. With
HoneySQL, one don't need to format strings to build a SQL expression, which is
clumsy and dangerous in terms of SQL injections.

The package also provides several shortcuts for such common dutiles as get a
single row by id, get a bunch of rows by their ids, insert a row having a map of
values, update by a column->value map and so on.

Before we go though the demo, let's do some preparetion first. Import the
package, declare a config map and create a table with some rows as follows:

~~~clojure
(require '[pg.honey :as pgh])

(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"})

(def conn
  (pg/connect config))

(pg/query conn "create table test003 (
  id integer not null,
  name text not null,
  active boolean not null default true
)")

(pg/query conn "insert into test003 (id, name, active)
  values
  (1, 'Ivan', true),
  (2, 'Huan', false),
  (3, 'Juan', true)")
~~~

Now we're ready for the demo.

### Get by id

The `get-by-id` function fetches a single row by its primary key which is `:id`
by default:

~~~clojure
(pgh/get-by-id conn :test003 1)
;; {:name "Ivan", :active true, :id 1}
~~~

With options, you can specify the name of the primary key and the column names
you're interested in:

~~~clojure
(pgh/get-by-id conn
               :test003
               1
               {:pk [:raw "test003.id"]
                :fields [:id :name]})

;; {:name "Ivan", :id 1}

;; SELECT id, name FROM test003 WHERE test003.id = $1 LIMIT $2
;; parameters: $1 = '1', $2 = '1'
~~~

### Get by ids

The `get-by-ids` function accepts a collection of primary keys and fetch them
using the `IN` operator. In additon to options that `get-by-id` has, you can
specify the ordering:

~~~clojure
(pgh/get-by-ids conn
                :test003
                [1 3 999]
                {:pk [:raw "test003.id"]
                 :fields [:id :name]
                 :order-by [[:id :desc]]})

[{:name "Juan", :id 3}
 {:name "Ivan", :id 1}]

;; SELECT id, name FROM test003 WHERE test003.id IN ($1, $2, $3) ORDER BY id DESC
;; parameters: $1 = '1', $2 = '3', $3 = '999'
~~~

Passing many IDs at once is not recommended. Either pass them by chunks or
create a temp table, copy IDs there and join it with the main table.

### Delete

The `delete` function removes rows from a table. By default, all the rows are
deleted with no filtering, and the deleted rows are returned:

~~~clojure
(pgh/delete conn :test003)

[{:name "Ivan", :active true, :id 1}
 {:name "Huan", :active false, :id 2}
 {:name "Juan", :active true, :id 3}]
~~~

You can specify the `WHERE` clause and the column names of the result:

~~~clojure
(pgh/delete conn
            :test003
            {:where [:and
                     [:= :id 3]
                     [:= :active true]]
             :returning [:*]})

[{:name "Juan", :active true, :id 3}]
~~~

When passing the `:returning` option set to `nil`, no rows are returned.

### Insert

To observe all the features of the `insert` function, let's create a separate
table:

~~~clojure
(pg/query conn "create table test004 (
  id serial primary key,
  name text not null,
  active boolean not null default true
)")
~~~

The function accepts a collection of maps where each map represents a row:

~~~clojure
(pgh/insert conn
            :test004
            [{:name "Foo" :active false}
             {:name "Bar" :active true}]
            {:returning [:id :name]})

[{:name "Foo", :id 1}
 {:name "Bar", :id 2}]
~~~

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

## Cloning a Connectin

## Cancelling a Query

## Thread Safety

## Result reducers

## COPY IN/OUT

## SSL

## Type Mapping

## JSON support

## Arrays support

## Notify/Listen (PubSub)

## Notices

## Logging

## Errors and Exceptions

## Connection Pool

## Component integration

## Ring middleware

## Debugging

## Running tests

## Running benchmarks
