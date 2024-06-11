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

- [Installation](/docs/installation.md)
- [Quick start (Demo)](/docs/quick-start.md)
- [Benchmarks](/docs/benchmarks.md)
- [Authentication](/docs/authentication.md)
- [Connecting to the server](/docs/connecting.md)
- [Query and Execute](/docs/query-execute.md)
- Common Execute parameters
- Type hints
- [Prepared Statements](/docs/prepared-statement.md)
- [Transactions](/docs/transaction.md)
- [Connection state](/docs/connection-state.md)
- [HoneySQL Integration](/docs/honeysql.md)
- [HugSQL Support](/docs/hugsql.md)
- [Next.JDBC API layer](/docs/next-jdbc-layer.md)
- Working with Enums
- Cloning a Connection
- Cancelling a Query
- Thread Safety
- Result reducers
- COPY IN/OUT
- SSL Support
- Type Mapping
- [JSON support](/docs/json.md)
- [Arrays support](/docs/arrays.md)
- Notify/Listen
- Notices
- Logging
- Errors and Exceptions
- Connection Pool
- [Migrations](/docs/migrations.md)
- Component integration
- Ring middleware
- Debugging
- Running tests
