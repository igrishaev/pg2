# PG2: *Fast* PostgreSQL driver for Clojure

`PG2` is a JDBC-free PostgreSQL client library.

**It's fast**: [benchmarks](/docs/benchmarks.md) prove up to 3 times performance boost compared to
`next.jdbc`. Simple HTTP application reading from the database and
serving JSON handles 2 times more RPS.

**It's written in Java** with a Clojure layer on top of it. Unfortunately
Clojure is not as fast as Java so most of the logic had to be implemented in pure Java. The Clojure layer is extremely thin and serves only
to call certain methods.

**It's Clojure friendly**: by default all queries return vector of
maps with keys as keywords. Also `PG2` provides dozens of ways to group, index, and reduce
a result.

**It supports JSON out of the box**: there is no need to extend any protocols. Read and write JSON(B) as Clojure data with ease! Also JSON reads/writes are *really fast* again: 2-3 times faster than in `next.jdbc`.

**It supports `COPY` operations**: you can easily `COPY OUT` a table into a
stream. You can `COPY IN` a set of rows without CSV-encoding them because it's
held by the library. It also supports binary `COPY` format which is faster.

**It supports `java.time.*`** classes. The ordinary JDBC clients still use
`Timestamp` for dates which is horrible. In `PG2` all the `java.time.*`
classes are supported for both reading and writing.

...and plenty of other features!

## Documentation

- [Installation](/docs/installation.md)
- [Quick start (Demo)](/docs/quick-start.md)
- [Benchmarks](/docs/benchmarks.md)
- [Authentication](/docs/authentication.md)
- [Connecting to the server](/docs/connecting.md)
- [Query and Execute API](/docs/query-execute.md)
- [Prepared Statements](/docs/prepared-statement.md)
- [Transactions](/docs/transaction.md)
- [Connection state](/docs/connection-state.md)
- [Connection Pool](/docs/pool.md)
- [HoneySQL Integration](/docs/honeysql.md)
- [HugSQL Support](/docs/hugsql.md)
- [next.jdbc API layer](/docs/next-jdbc-layer.md)
- [Folders (Reducers)](/docs/folders.md)
- [JSON support](/docs/json.md)
- [Arrays support](/docs/arrays.md)
- [Migrations](/docs/migrations.md)
- Common Execute parameters
- Type hints
- Working with Enums
- Cloning a Connection
- Cancelling a Query
- Thread Safety
- COPY IN/OUT
- SSL Support
- Type Mapping
- Notify/Listen
- Notices
- Logging
- Errors and Exceptions
- Component integration
- Ring middleware
- Debugging
- Running tests
