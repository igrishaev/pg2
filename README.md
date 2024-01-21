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
- [Connecting the server](#connecting-the-server)
- [Query and Execute](#query-and-execute)
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
[com.github.igrishaev/pg2-core "0.1.1"]

;; deps
com.github.igrishaev/pg2-core {:mvn/version "0.1.1"}
~~~

**HoneySQL integration**: special version of `query` and `execute` that accept
not a string of SQL but a map that gets formatted to SQL under the hood.

~~~clojure
;; lein
[com.github.igrishaev/pg2-honey "0.1.1"]

;; deps
com.github.igrishaev/pg2-honey {:mvn/version "0.1.1"}
~~~

[component]: https://github.com/stuartsierra/component

**Component integration**: a package that extends the `Connection` and `Pool`
objects with the `Lifecycle` protocol from the [Component][component] library.

~~~clojure
;; lein
[com.github.igrishaev/pg2-component "0.1.1"]

;; deps
com.github.igrishaev/pg2-component {:mvn/version "0.1.1"}
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


;; The result: pay attention we got java.time.OffsetDateTime for timestamptz column:
[{:title "test1",
  :id 4,
  :created_at
  #object[java.time.OffsetDateTime 0x31340eb6 "2024-01-17T21:57:58.660012+03:00"]}
 {:title "test2",
  :id 5,
  :created_at
  #object[java.time.OffsetDateTime 0x11a5aab5 "2024-01-17T21:57:58.660012+03:00"]}
 {:title "test3",
  :id 6,
  :created_at
  #object[java.time.OffsetDateTime 0x3ee200bc "2024-01-17T21:57:58.660012+03:00"]}]


;; Try two expressions in a single transaction
(pg/with-tx [conn]
  (pg/execute conn
              "delete from demo where id = $1"
              {:params [3]})
  (pg/execute conn
              "insert into demo (title) values ($1)"
              {:params ["test4"]}))
;; {:inserted 1}


;; Check out the database log:

;; LOG:  statement: BEGIN
;; LOG:  execute s3/p4: delete from demo where id = $1
;; DETAIL:  parameters: $1 = '3'
;; LOG:  execute s5/p6: insert into demo (title) values ($1)
;; DETAIL:  parameters: $1 = 'test4'
;; LOG:  statement: COMMIT

~~~


## Benchmarks

## Authentication

## Connecting the server

## Query and Execute

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

## Connection Pool

## HoneySQL integration

## Component integration

## Ring middleware

## Debugging

## Running tests

## Running benchmarks
