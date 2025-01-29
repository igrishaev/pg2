# Data Source Abstraction

A recent release of PG2 introduces something called a data source. Brifely, it's
when a function accepts not a `Connection` instance only but rather an object
implementing the `org.pg.ISource` protocol. It makes the function work with a
connection pool, a Clojure map, or a URI string as well.

For example, before, the `query` function accepted a connection only. You should
have opened a connection first and pass it into the `query` function:

~~~clojure
(def config {...})

(pg/with-conn [conn config]
  (pg/query conn "select 42"))
~~~

Now you can pass a map directly to `query`:

~~~clojure
(pg/query config "select 42")
~~~

Under the hood, the library spawns a new connection from this map, performs a
query and closes the connection afterwards. Of course, opening and closing a
connection every time is much slower that reusing the same one, especially in a
loop. But it gives you more freedom, and sometimes it is fine in tests.

What's important, you can pass a `Pool` instance into the `query` and `execute`
functions as well. For a pool, the function borrows a connection first, making
it unavailable for consumers in other threads. Once the query is done, the
connection is returned to the pool, meaning it's now available for use.

~~~clojure
(def pool (pg/pool {...}))

(pg/query pool "select 42")
~~~

See the [Connection Pool](/docs/pool.md) section for more details about a
connection pool.

You can pass a URI string as well:

~~~clojure
(def URI "postgresql://test:test@127.0.0.1:5432/test?ssl=false")

(pg/query URI "select 1")
~~~

See the [URI Connection String](/docs/connection-uri.md) section for details.

# Supported Sources

The following objects may act as a data source:

| type                          | Borrowing logic               | Returning logic           |
|-------------------------------|-------------------------------|---------------------------|
| `org.pg.Connection`           | Return itself                 | Nothing (keep it open)    |
| `org.pg.Pool`                 | Borrow one from a pool (lock) | Return to a pool (unlock) |
| `clojure.lang.IPersistentMap` | Connect using a Clojure map   | Close the connection      |
| `java.lang.String`            | Connect using a URI           | Close the connection      |
| `org.pg.Config`               | Connect using a Config object | Close the connection      |

# Naming Rules

Some `pg.core` functions accept any data source, but some require a `Connection`
object only. For example, `query`, `execute`, `copy`, etc accept a source, but
`close-statement` needs a connection because prepared statements are defined per
connection. There is a naming rule for that:

- if the first argument is called `src`, the function accepts a data source (a
  connection, a pool, a Clojure map, a URI string);
- if the argument is called `conn`, it accepts a connection only. Most likely
  you need to pass a certain connection, for example the one you used for a
  prepared statement or to process notifications.
