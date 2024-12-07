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

## Documentation

[tests]: https://github.com/igrishaev/pg2/blob/master/pg-core/test/pg/client_test.clj

*A note:* sections with no links mean they're implemented yet not documented. If
you're interested in a non-documented feature, either ping me or [check out the
tests][tests].

- [Installation](/docs/installation.md)
- [Quick Start (Demo)](/docs/quick-start.md)
- [Benchmarks](/docs/benchmarks.md)
- [Authentication](/docs/authentication.md)
- [Connecting to the Server](/docs/connecting.md)
- [Query and Execute](/docs/query-execute.md)
- [Prepared Statements](/docs/prepared-statement.md)
- [Transactions](/docs/transaction.md)
- [Connection State](/docs/connection-state.md)
- [Connection Pool](/docs/pool.md)
- [HoneySQL Integration](/docs/honeysql.md)
- [HugSQL Support](/docs/hugsql.md)
- [Next.JDBC API layer](/docs/next-jdbc-layer.md)
- [Folders (Reducers)](/docs/folders.md)
- [JSON Support](/docs/json.md)
- [Arrays Support](/docs/arrays.md)
- [Migrations](/docs/migrations.md)
- Common Execute Parameters
- Type Hints (OIDs)
- Types & Extensions
  - [Geometry (line, box, etc)](/docs/geometry.md)
  - [PGVector](/docs/pgvector.md)
  - PostGIS
  - [Custom Type Processors](/docs/processors.md)
- Working with Enums
- Cloning a Connection
- Cancelling a Query
- Thread Safety
- COPY FROM/TO
- [SSL Setup](/docs/ssl.md)
- [Services Tested With](/docs/services.md)
- Type Mapping
- Notify/Listen
- Notices
- Logging
- Errors and Exceptions
- Component Integration
- Ring middleware
- Debugging
- Running Tests
