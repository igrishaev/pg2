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

...And plenty of more features.

## Table of Contents

<!-- toc -->

- [Installation](#installation)
- [Quick start](#quick-start)
- [Benchmarks](#benchmarks)
- [Connecting to the server](#connecting-to-the-server)
- [Authentication](#authentication)
- [Query and Execute](#query-and-execute)
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
- [Running tests](#running-tests)
- [Running benchmarks](#running-benchmarks)

<!-- tocstop -->

## Installation

## Quick start

## Benchmarks

## Connecting to the server

## Authentication

## Query and Execute

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

## Running tests

## Running benchmarks
