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
- [Connection parameters](#connection-parameters)
- [Query and Execute](#query-and-execute)
- [Enum types](#enum-types)
- [Prepared Statements](#prepared-statements)
- [Transactions](#transactions)
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

See the folling posts in my blog:

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

## Connection parameters

The following table describes all the possible connection options with the
possible values and semantics.

| Field             | Type/enum | Default   | Comment                                      |
|-------------------|-----------|-----------|----------------------------------------------|
| `:user`           | string    | required  | the name of the DB user                      |
| `:database`       | string    | required  | the name of the database                     |
| `:host`           | string    | 127.0.0.1 | IP or hostname                               |
| `:port`           | integer   | 5432      | port number                                  |
| `:password`       | string    | ""        | DB user password                             |
| `:pg-params`      | map       | {}        | A map of session params like {string string} |
| `:binary-encode?` | bool      | false     | Whether to use binary data encoding          |
| `:binary-decode?` | bool      | false     | Whether to use binary data decoding          |
|                   |           |           |                                              |


## Query and Execute

## Enum types

## Prepared Statements

## Transactions

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

## HoneySQL integration

## Component integration

## Ring middleware

## Debugging

## Running tests

## Running benchmarks
