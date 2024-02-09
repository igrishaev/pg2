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
- [Type hints](#type-hints)
- [Prepared Statements](#prepared-statements)
- [Transactions](#transactions)
- [Enums](#enums)
- [Cloning a Connectin](#cloning-a-connectin)
- [Cancelling a Query](#cancelling-a-query)
- [Thread Safety](#thread-safety)
- [Result reducers](#result-reducers)
- [COPY IN/OUT](#copy-inout)
- [SSL](#ssl)
- [Connection state](#connection-state)
- [Type Mapping](#type-mapping)
- [JSON support](#json-support)
- [Arrays support](#arrays-support)
- [Notify/Listen (PubSub)](#notifylisten-pubsub)
- [Notices](#notices)
- [Logging](#logging)
- [Errors and Exceptions](#errors-and-exceptions)
- [Connection Pool](#connection-pool)
- [HoneySQL integration](#honeysql-integration)
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
[com.github.igrishaev/pg2-core "0.1.2"]

;; deps
com.github.igrishaev/pg2-core {:mvn/version "0.1.2"}
~~~

**HoneySQL integration**: special version of `query` and `execute` that accept
not a string of SQL but a map that gets formatted to SQL under the hood.

~~~clojure
;; lein
[com.github.igrishaev/pg2-honey "0.1.2"]

;; deps
com.github.igrishaev/pg2-honey {:mvn/version "0.1.2"}
~~~

[component]: https://github.com/stuartsierra/component

**Component integration**: a package that extends the `Connection` and `Pool`
objects with the `Lifecycle` protocol from the [Component][component] library.

~~~clojure
;; lein
[com.github.igrishaev/pg2-component "0.1.2"]

;; deps
com.github.igrishaev/pg2-component {:mvn/version "0.1.2"}
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

## Type hints

## Prepared Statements

## Transactions

## Enums

## Cloning a Connectin

## Cancelling a Query

## Thread Safety

## Result reducers

## COPY IN/OUT

## SSL

## Connection state

## Type Mapping

## JSON support

## Arrays support

## Notify/Listen (PubSub)

## Notices

## Logging

## Errors and Exceptions

## Connection Pool

## HoneySQL integration

## Component integration

## Ring middleware

## Debugging

## Running tests

## Running benchmarks
