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
  * [Get by id(s)](#get-by-ids)
  * [Delete](#delete)
  * [Insert (one)](#insert-one)
  * [Update](#update)
  * [Find (first)](#find-first)
  * [Prepare](#prepare)
  * [Query and Execute](#query-and-execute)
  * [HoneySQL options](#honeysql-options)
- [next.JDBC API layer](#nextjdbc-api-layer)
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
- [Migrations](#migrations)
  * [Concepts](#concepts)
  * [Naming](#naming)
  * [SQL](#sql)
  * [No Code-Driven Migrations](#no-code-driven-migrations)
  * [Migration Resources](#migration-resources)
  * [Migration Table](#migration-table)
  * [CLI Interface](#cli-interface)
  * [Config](#config)
  * [Commands](#commands)
  * [Lein examples](#lein-examples)
  * [Deps.edn examples](#depsedn-examples)
  * [API Interface](#api-interface)
  * [Conflicts](#conflicts)
- [Debugging](#debugging)
- [Running tests](#running-tests)
- [Running benchmarks](#running-benchmarks)

<!-- tocstop -->

## Installation

**Core functionality**: the client and the connection pool, type encoding and
decoding, COPY IN/OUT, SSL:

~~~clojure
;; lein
[com.github.igrishaev/pg2-core "0.1.4"]

;; deps
com.github.igrishaev/pg2-core {:mvn/version "0.1.4"}
~~~

**HoneySQL integration**: special version of `query` and `execute` that accept
not a string of SQL but a map that gets formatted to SQL under the hood. Also
includes various helpers (`get-by-id`, `find`, `insert`, `udpate`, `delete`,
etc).

~~~clojure
;; lein
[com.github.igrishaev/pg2-honey "0.1.4"]

;; deps
com.github.igrishaev/pg2-honey {:mvn/version "0.1.4"}
~~~

[component]: https://github.com/stuartsierra/component

**Component integration**: a package that extends the `Connection` and `Pool`
objects with the `Lifecycle` protocol from the [Component][component] library.

~~~clojure
;; lein
[com.github.igrishaev/pg2-component "0.1.4"]

;; deps
com.github.igrishaev/pg2-component {:mvn/version "0.1.4"}
~~~

**Migrations**: a package that provides migration management: migrate forward,
rollback, create, list applied migrations and so on.

~~~clojure
;; lein
[com.github.igrishaev/pg2-migration "0.1.5]

;; deps
com.github.igrishaev/pg2-migration {:mvn/version "0.1.5"}
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

The `pg-honey` package allows you to call `query` and `execute` functions using
maps rather than string SQL expressions. Internally, maps are transformed into
SQL using the great [HoneySQL library][honeysql]. With HoneySQL, you don't need
to format strings to build a SQL, which is clumsy and dangerous in terms of
injections.

The package also provides several shortcuts for such common dutiles as get a
single row by id, get a bunch of rows by their ids, insert a row having a map of
values, update by a map and so on.

For a demo, let's import the package, declare a config map and create a table
with some rows as follows:

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

### Get by id(s)

The `get-by-id` function fetches a single row by a primary key which is `:id` by
default:

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

The `get-by-ids` function accepts a collection of primary keys and fetches them
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
create a temporary table, `COPY IN` ids into it and `INNER JOIN` with the main
table.

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

When the `:returning` option set to `nil`, no rows are returned.

### Insert (one)

To observe all the features of the `insert` function, let's create a separate
table:

~~~clojure
(pg/query conn "create table test004 (
  id serial primary key,
  name text not null,
  active boolean not null default true
)")
~~~

The `insert` function accepts a collection of maps each represents a row:

~~~clojure
(pgh/insert conn
            :test004
            [{:name "Foo" :active false}
             {:name "Bar" :active true}]
            {:returning [:id :name]})

[{:name "Foo", :id 1}
 {:name "Bar", :id 2}]
~~~

It also accepts options to produce the `ON CONFLICT ... DO ...`  clause known as
`UPSERT`. The following query tries to insert two rows with existing primary
keys. Should they exist, the query updates the names of the corresponding rows:

~~~clojure
(pgh/insert conn
            :test004
            [{:id 1 :name "Snip"}
             {:id 2 :name "Snap"}]
            {:on-conflict [:id]
             :do-update-set [:name]
             :returning [:id :name]})
~~~

The resulting query looks like this:

~~~sql
INSERT INTO test004 (id, name) VALUES ($1, $2), ($3, $4)
  ON CONFLICT (id)
  DO UPDATE SET name = EXCLUDED.name
  RETURNING id, name
parameters: $1 = '1', $2 = 'Snip', $3 = '2', $4 = 'Snap'
~~~

The `insert-one` function acts like `insert` but accepts and returns a single
map. It supports `:returning` and `ON CONFLICT ...` clauses as well:

~~~clojure
(pgh/insert-one conn
                :test004
                {:id 2 :name "Alter Ego" :active true}
                {:on-conflict [:id]
                 :do-update-set [:name :active]
                 :returning [:*]})

{:name "Alter Ego", :active true, :id 2}
~~~

The logs:

~~~sql
INSERT INTO test004 (id, name, active) VALUES ($1, $2, TRUE)
  ON CONFLICT (id)
  DO UPDATE SET name = EXCLUDED.name, active = EXCLUDED.active
  RETURNING *
parameters: $1 = '2', $2 = 'Alter Ego'
~~~

### Update

The `update` function alters rows in a table. By default, it doesn't do any
filtering and returns all the rows affected. The following query sets the
boolean `active` value for all rows:

~~~clojure
(pgh/update conn
            :test003
            {:active true})

[{:name "Ivan", :active true, :id 1}
 {:name "Huan", :active true, :id 2}
 {:name "Juan", :active true, :id 3}]
~~~

The `:where` clause determines conditions for update. You can also specify
columns to return:

~~~clojure
(pgh/update conn
            :test003
            {:active false}
            {:where [:= :name "Ivan"]
             :returning [:id]})

[{:id 1}]
~~~

What is great about `update` is, you can use such complex expressions as
increasing counters, negation and so on. Below, we alter the primary key by
adding 100 to it, negate the `active` column, and change the `name` column with
dull concatenation:

~~~clojure
(pgh/update conn
            :test003
            {:id [:+ :id 100]
             :active [:not :active]
             :name [:raw "name || name"]}
            {:where [:= :name "Ivan"]
             :returning [:id :active]})

[{:active true, :id 101}]
~~~

Which produces the following query:

~~~sql
UPDATE test003
  SET
    id = id + $1,
    active = NOT active,
    name = name || name
  WHERE name = $2
  RETURNING id, active
parameters: $1 = '100', $2 = 'Ivan'
~~~

### Find (first)

The `find` function makes a lookup in a table by column-value pairs. All the
pairs are joined using the `AND` operator:

~~~clojure
(pgh/find conn :test003 {:active true})

[{:name "Ivan", :active true, :id 1}
 {:name "Juan", :active true, :id 3}]
~~~

Find by two conditions:

~~~clojure
(pgh/find conn :test003 {:active true
                         :name "Juan"})

[{:name "Juan", :active true, :id 3}]

;; SELECT * FROM test003 WHERE (active = TRUE) AND (name = $1)
;; parameters: $1 = 'Juan'
~~~

The function accepts additional options for `LIMIT`, `OFFSET`, and `ORDER BY`
clauses:

~~~clojure
(pgh/find conn
          :test003
          {:active true}
          {:fields [:id :name]
           :limit 10
           :offset 1
           :order-by [[:id :desc]]
           :fn-key identity})

[{"id" 1, "name" "Ivan"}]

;; SELECT id, name FROM test003
;;   WHERE (active = TRUE)
;;   ORDER BY id DESC
;;   LIMIT $1
;;   OFFSET $2
;; parameters: $1 = '10', $2 = '1'
~~~

The `find-first` function acts the same but returns a single row or
`nil`. Internally, it adds the `LIMIT 1` clause to the query:

~~~clojure
(pgh/find-first conn :test003
                {:active true}
                {:fields [:id :name]
                 :offset 1
                 :order-by [[:id :desc]]
                 :fn-key identity})

{"id" 1, "name" "Ivan"}
~~~

### Prepare

The `prepare` function makes a prepared statement from a HoneySQL map:

~~~clojure
(def stmt
  (pgh/prepare conn {:select [:*]
                     :from :test003
                     :where [:= :id 0]}))

;; <Prepared statement, name: s37, param(s): 1, OIDs: [INT8], SQL: SELECT * FROM test003 WHERE id = $1>
~~~

Above, the zero value is a placeholder for an integer parameter.

Now that the statement is prepared, execute it with the right id:

~~~clojure
(pg/execute-statement conn stmt {:params [3]
                                 :first? true})

{:name "Juan", :active true, :id 3}
~~~

Alternately, use the `[:raw ...]` syntax to specify a parameter with a dollar
sign:

~~~clojure
(def stmt
  (pgh/prepare conn {:select [:*]
                     :from :test003
                     :where [:raw "id = $1"]}))

(pg/execute-statement conn stmt {:params [1]
                                 :first? true})

{:name "Ivan", :active true, :id 1}
~~~

### Query and Execute

There are two general functions called `query` and `execute`. Each of them
accepts an arbitrary HoneySQL map and performs either `Query` or `Execute`
request to the server.

Pay attention that, when using `query`, a HoneySQL map cannot have
parameters. This is a limitation of the `Query` command. The following query
will lead to an error response from the server:

~~~clojure
(pgh/query conn
           {:select [:id]
            :from :test003
            :where [:= :name "Ivan"]
            :order-by [:id]})

;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:207).
;; Server error response: {severity=ERROR, ... message=there is no parameter $1, verbosity=ERROR}
~~~

Instead, use either `[:raw ...]` syntax or `{:inline true}` option:

~~~clojure
(pgh/query conn
           {:select [:id]
            :from :test003
            :where [:raw "name = 'Ivan'"] ;; raw (as is)
            :order-by [:id]})

[{:id 1}]

;; OR

(pgh/query conn
           {:select [:id]
            :from :test003
            :where [:= :name "Ivan"]
            :order-by [:id]}
           {:honey {:inline true}}) ;; inline values

[{:id 1}]

;; SELECT id FROM test003 WHERE name = 'Ivan' ORDER BY id ASC
~~~

The `execute` function acceps a HoneySQL map with parameters:

~~~clojure
(pgh/execute conn
               {:select [:id :name]
                :from :test003
                :where [:= :name "Ivan"]
                :order-by [:id]})

[{:name "Ivan", :id 1}]
~~~

Both `query` and `execute` accept not `SELECT` only but literally everything:
inserting, updating, creating a table, an index, and more. You can build
combinations like `INSERT ... FROM SELECT` or `UPDATE ... FROM DELETE` to
perform complex logic in a single atomic query.

### HoneySQL options

Any HoneySQL-specific parameter might be passed through the `:honey` submap in
options. Below, we pass the `:params` map to use the `[:param ...]`
syntax. Also, we produce a pretty-formatted SQL for better logs:

~~~clojure
(pgh/execute conn
             {:select [:id :name]
              :from :test003
              :where [:= :name [:param :name]]
              :order-by [:id]}
             {:honey {:pretty true
                      :params {:name "Ivan"}}})

;; SELECT id, name
;; FROM test003
;; WHERE name = $1
;; ORDER BY id ASC
;; parameters: $1 = 'Ivan'
~~~

For more options, please refer to the official [HoneySQL
documentation][honeysql].

## next.JDBC API layer

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

## Migrations

The project provides its own migration engine through the `pg2-migration`
package (see the Installation section). Like Migratus or Ragtime, it allows to
grow the database schema continuously, track changes and apply them with care.

### Concepts

Migrations are SQL files that get applied to the database in certain order. A
migration has an id and a direction: next/up or prev/down. Usually it's split on
two files called `<id>.up.sql` and `<id>.down.sql` each of them holding SQL
commands. Say, the -up file creates a table with an index, and the -down one
drops the index first, and then the table.

Migrations might have a slug: a short and human-friendly text describing
changes. For example, in a file called `002.create-users-table.up.sql`, the slug
is "Create users table".

### Naming

In PG2, the migration framework looks for files named according to the following
pattern:

~~~
<id>.<slug>.<direction>.sql
~~~

where:

- `id` is a Long number, for example 12345 (a counter), or 20240311 (date
  precision), or 20240311235959 (date & time precision);

- `slug` is an optional word or group of words joined with - or _, for example
  `create-users-table-and-index` or `remove_some_view`. When rendered, both -
  and _ are substituted with spaces, and the phrase is capitalized.

- `direction` is either `prev/down` or `next/up`. Internally, `down` and `up`
  are transformed to `prev` and `next` because these two have the same amount of
  characters which makes file look better.

Examples:

- `001-create-users.next.sql`
- `012-next-only-migration.up.sql`
- `153-add-some-table.next.sql`

Above, the leading zeroes in ids are used for better alignment only. Infernally
they are transferred into 1, 12 and 153 Long numbers. Thus, `001`, `01` and `1`
become the same id 1 after parsing.

Each id has at most two directions: prev/down and next/up. On bootstrap, the
engine checks that constraint to prevent weird behaviour. The table below shows
that there are two rows which, after parsing, have the same (id, direction)
pair. The bootstrap step will end up with an exception saying which files
duplicate each other.

| Filename                         | Parsed    |
|----------------------------------|-----------|
| `001-some-trivial-slug.next.sql` | (1, next) |
| `001-some-simple-slug.next.sql`  | (1, next) |

A migration might have only one direction, e.g. next/up or prev/down file only.

When parsing, the registry gets ignored meaning `001-Create-Users.NEXT.sql` and
`001-CREATE-USERS.next.SQL` produce the same maps.

### SQL

The files hold SQL expressions to be evaluated by the engine. Here is the
content of the `001-create-users.next.sql` file:

~~~sql
create table IF NOT EXISTS test_users (
  id serial primary key,
  name text not null
);

begin;

insert into test_users (name) values ('Ivan');
insert into test_users (name) values ('Huan');
insert into test_users (name) values ('Juan');

commit;
~~~

Pay attention to the following features.

- A single file might have as many SQL expressions as you want. There is no need
  to separate them with magic comments like `--;;` as Migratus requires. The
  whole file is executed in a single query. Use the standard semicolon at the
  end of each expression.

- There is no a hidden transaction management. Transactions are up to you:
  everything is explicit! Above, we wrap tree `INSERT` expressions into a
  transaction. You can use save-points, rollbacks, or whatever you want. Note
  that not all expressions can be inside a transaction. Say, the `CREATE TABLE`
  one cannot and thus is out from the transaction scope.

For granular transaction control, split your complex transaction on two or three
files named like:

```
# direct parts
001-huge-update-step-1.next.sql
002-huge-update-step-2.next.sql
003-huge-update-step-3.next.sql

# backward counterparts
003-huge-update-step-3.prev.sql
002-huge-update-step-2.prev.sql
001-huge-update-step-1.prev.sql
```

### No Code-Driven Migrations

At the moment, nither .edn nor .clj transactions are not supported. This is by
design as I'm highly agains mixing SQL and Clojure. Everytime I see an EDN
transaction that points to a Clojure function, I get angry. Mixing these two for
database management is the worst idea one can come up with. Please, if you're
thinking about migrating the database with Clojure, close you laptop and have a
walk to the nearest park.

### Migration Resources

Migration files are stored in project resources. The default search path is
`migrations`. Thus, their physical location is `resources/migrations`. The
engine scans the `migrations` resource for children files. Files from nested
directories are also taken into account.

The engine supports Jar resources when running the code from uberjar. The
resource path can be overridden with settings.

### Migration Table

All the applied migrations are tracked in a database table called `migrations`
by default. The engine saves the id and the slug or a migration applied as well
as the current datetime of the event. The datetime field has the time zone. Here
is the structure of the table:

~~~sql
CREATE TABLE IF NOT EXISTS migrations (
  id BIGINT PRIMARY KEY,
  slug TEXT,
  created_at timestamp with time zone not null default current_timestamp
)
~~~

Every time you apply a migration, a new record is inserted into the table. On
rollback, the corresponding migration is deleted.

You can override the name of the table in settings (see below).

### CLI Interface

The migration engine is controlled with both API and CLI interface. We will
start with CLI first.

The `pg.migration.cli` namespaces acts like an entry point. It accepts general
options, a command, and command-specific options:

```
<global options> <command> <command options>
```

General options are:

```
-c, --config CONNFIG                               Path to the .edn config file
-p, --port PORT          5432                      Port number
-h, --host HOST          localhost                 Host name
-u, --user USER          The current USER env var  User
-w, --password PASSWORD  <empty string>            Password
-d, --database DATABASE  The current USER env var  Database
    --table TABLE        :migrations               Migrations table
    --path PATH          migrations                Migrations path
```

Most of the options have default values. Both name of the user and the database
come from the HOME `environment` variable. The password is an empty string by
default. For local trusted connections, the password might not be required.

The list of the commands:

| Name     | Meaning                                                        |
|----------|----------------------------------------------------------------|
| create   | Create a pair of blank up & down migration files               |
| help     | Pring the help message                                         |
| list     | Show all the migrations and their status (applied or not)      |
| migrate  | Migrate forward (everything, next only, or up to a certain ID) |
| rollback | Rollback (the current, everything, or down to a certain ID)    |

Each command has its own sup-options which we will describe below.

Here is how you review the migrations:

~~~
<lein or deps preamble> \
    -h 127.0.0.1 \
    -p 10150 \
    -u test \
    -w test \
    -d test \
    --table migrations_test \
    --path migrations \
    list

|    ID | Applied? | Slug
| ----- | -------- | --------
|     1 | true     | create users
|     2 | false    | create profiles
|     3 | false    | next only migration
|     4 | false    | prev only migration
|     5 | false    | add some table
~~~

Every command has its own arguments and help message. For example, to review the
`create` command, run:

~~~
lein with-profile +migrations run -m pg.migration.cli -c config.example.edn create --help

Syntax:
      --id ID             The id of the migration (auto-generated if not set)
      --slug SLUG         Optional slug (e.g. 'create-users-table')
      --help       false  Show help message
~~~

### Config

Passing `-u`, `-h`, and other arguments all the time is inconvenient. The engine
can read all them at once from a config file. The default path is
`migration.config.edn`. Override path to the config using the `-c` parameter:

~~~
<lein/deps> -c config.edn list
~~~

The config file has the following structure:

~~~clojure
{:host "127.0.0.1"
 :port 10150
 :user "test"
 :password #env PG_PASSWORD
 :database "test"
 :migrations-table :migrations_test
 :migrations-path "migrations"}
~~~

The `:migrations-table` field must be a keyword and it takes place in a HoneySQL
map.

The `:migrations-path` field is a string referencing a resource with migrations.

To not expose the `:password` field, it's wrapped with the `#env` tag. The
engine reads the "PG_PASSWORD" environment variable for password. When it's not
set, an exception is thrown.

### Commands

#### Create

The `create` command makes a pair of two blank migration files. Then id, if not
set, is generated automatically using the `YYYYmmddHHMMSS` pattern.

~~~
lein with-profile +migration run -m pg.migration.cli \
  -c config.example.edn \
  create

ls -l migrations

20240312074156.next.sql
20240312074156.prev.sql
~~~

You can also provide a custom id and a slug as well:

~~~
lein with-profile +migration run -m pg.migration.cli \
  -c config.example.edn \
  create \
  --id 100500 \
  --slug 'some huge changes in tables'

ll migrations

100500.some-huge-changes-in-tables.next.sql
100500.some-huge-changes-in-tables.prev.sql
20240312074156.next.sql
20240312074156.prev.sql
~~~

#### List

The `list` command renders all the migrations stored in resources and their
status: whether they are applied or not.

~~~clojure
lein with-profile +migration run -m pg.migration.cli -c config.example.edn list

|    ID | Applied? | Slug
| ----- | -------- | --------
|     1 | true     | create users
|     2 | true     | create profiles
|     3 | true     | next only migration
|     4 | false    | prev only migration
|     5 | false    | add some table
~~~

#### Migrate

The `migrate` command applies pending migrations to the database. By default,
all the migrations that have not been applied are processed. You can change this
behaviour using these flags:

~~~
... -c config.example.edn migrate --help

Syntax:
      --all           Migrate all the pending migrations
      --one           Migrate next a single pending migration
      --to ID         Migrate next to certain migration
      --help   false  Show help message
~~~

The `--one` flag means, only one next migration will be applied. If `--to`
parameter set, only migrations up to this given ID are processed. Examples:

~~~bash
... migrate
... migrate --all
... migrate --one
... migrate --to 123
~~~

#### Rollback

The `rollback` command applies reverting changes to the database and removes a
corresponding record from the table. By default, only the current migration is
rolled back.

~~~
... -c config.example.edn rollback --help

Syntax:
      --all           Rollback all the previous migrations
      --one           Rollback to the previous migration
      --to ID         Rollback to certain migration
      --help   false  Show help message
~~~

The `--one` argument is the default behaviour. When `--all` is passed, all the
backward migrations are processed. To rollback to a certain migration, pass
`--to ID`. Examples:

~~~
... rollback
... rollback --one
... rollback --to 20240515
... rollback --all
~~~

### Lein examples

Lein preamble is usually something like this:

~~~
> lein run -m pg.migration.cli <ARGS>
~~~

The `pg2-migration` library must be in dependencies. Sincle migrations are often
run aside from the main application, they're put into a separate profile, for
example:

~~~clojure
:profiles
{:migrations
 {:main pg.migration.cli
  :resource-paths ["extra/resources"]
  :dependencies
  [[com.github.igrishaev/pg2-core ...]]}}
~~~

Above, the `migrations` profile has the dependency and the `:main`
attribute. Now run `lein run` with migration args:

~~~bash
> lein with-profile +migrations run <options> <cmd> <cmd-opt>
~~~

For example:

~~~bash
> lein with-profile +migrations run -c custom.config.edn migrate --to 100500
~~~

### Deps.edn examples

Here is an example of an alias that shows pending migrations:

~~~clojure
{:aliases
 {:migrations-list
  {:extra-deps
   {com.github.igrishaev/pg2-migration {:mvn/version "..."}}
   :extra-paths
   ["test/resources"]
   :main-opts
   ["-m" "pg.migration.cli"
    "-h" "127.0.0.1"
    "-p" "10150"
    "-u" "test"
    "-w" "test"
    "-d" "test"
    "--table" "migrations_test"
    "--path" "migrations"
    "list"]}}}
~~~

Run it as follows:

~~~
> clj -M:migrations-list
~~~

Of course, you can shorten it using the config file. Move all the parameters
into the `migration.config.edn` file, and keep only commands in the `:main-opts`
vector:

~~~clojure
{:aliases
 {:migrations-migrate
  {:extra-deps
   {com.github.igrishaev/pg2-migration {:mvn/version "..."}}
   :extra-paths
   ["test/resources"]
   :main-opts ["migrate" "--all"]}}}
~~~

Migrate:

~~~
> clj -M:migrations-migrate
~~~

### API Interface

There is a way to call the migration engine in your code. The
`pg.migration.core` namespace provides basic functions to list, create, migrate,
and rollback migrations.

To migrate, call one of the following functions: `migrate-to`, `migrate-all`,
and `migrate-one`. All of them accept a config map:

~~~clojure
(ns demo
  (:require
   [pg.migration.core :as mig]))

(def CONFIG
  {:host "127.0.0.1"
   :port 5432
   :user "test"
   :password "secret"
   :database "test"
   :migrations-table :test_migrations
   :migrations-path "migrations"})

;; migrate all pinding migrations
(mig/migrate-all CONFIG)

;; migrate only one next migration
(mig/migrate-one CONFIG)

;; migrate to a certain migration
(mig/migrate-to CONFIG 20240313)
~~~

The same applies to rollback:

~~~clojure
;; rollback all previously applied migrations
(mig/rollback-all CONFIG)

;; rollback the current migration
(mig/migrate-one CONFIG)

;; rollback to the given migration
(mig/rollback-to CONFIG 20230228)
~~~

The `read-disk-migrations` function reads migrations from disk. It returns a
sorted map but without information about whether any migration has been applied:

~~~clojure
(mig/read-disk-migrations "migrations")

{1
 {:id 1
  :slug "create users"
  :url-prev #object[java.net.URL "file:/.../migrations/001-create-users.prev.sql"]
  :url-next #object[java.net.URL "file:/.../migrations/001-create-users.next.sql"]}
 2
 {:id 2
  :slug "create profiles"
  :url-prev #object[java.net.URL "file:/.../migrations/foobar/002-create-profiles.prev.sql"]
  :url-next #object[java.net.URL "file:/.../migrations/foobar/002-create-profiles.next.sql"]}
 ...}
~~~

The `make-scope` function accepts a config map and returns a scope map. The
scope map knows everything the state of migrations, namely: which of them have
been applied, what is the current migration, the table name, the resource path,
and more.

The function `create-migration-files` creates and returns a pair of empty SQL
files. By default, the id is generated from the current date & time, and the
slug is missing:

~~~clojure
(create-migration-files "migrations")

[#object[java.io.File "migrations/20240313120122.prev.sql"]
 #object[java.io.File "migrations/20240313120122.next.sql"]]

(create-migration-files "migrations" {:id 12345 :slug "Hello migration"})

[#object[java.io.File "migrations/12345.hello-migration.prev.sql"]
 #object[java.io.File "migrations/12345.hello-migration.next.sql"]]
~~~

### Conflicts

On bootstrap, the engine always checks migrations for conflicts. A conflict is a
situation when a migration with less id has been applied before a migration
with greater id. Usually it happens when two developers create migrations in
parallel and then merge then in a wrong order. For example:

- the latest migration id is 20240312;
- developer A makes a new branch and creates a migration 20240315;
- the next day, developer B opens a new branch with migration 20240316;
- dev. B merges his branch, now we have 20240312, then 20240316;
- dev. A merges his branch, and we have 20240312, 20240316, 20240315.

When you try to apply migration 20240315, the engine will check if 20240316 is
already applied. If yes, an exception pops up saying which migration causes the
problem (these are 20240316 and 20240315). To recover from the conflict, just
rename 20240315 to 20240317.

In other words: this is a conflict:

~~~
id        applied?
20240312  true
20240315  false
20240316  true
~~~

And this is a salutation:

~~~
id        applied?
20240312  true
20240316  true
20240317  false
~~~

## Debugging

## Running tests

## Running benchmarks
