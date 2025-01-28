# Data Source Abstraction

A recent release of PG2 has introduced something which is called a data
source. Brifely, it's when a function accepts not a `Connection` instance only
but rather an object implementing the `org.pg.ISource` protocol. It makes the
function work with a connection pool or a Clojure map as well.

For example, before, the `query` function accepted a connection only. You should
have open a connection first and pass it into the `query` function:

~~~clojure
(def config {...})

(pg/with-conn [conn config]
  (pg/query conn "select 42"))
~~~

Now you can pass a map directly to `query`:

~~~clojure
(pg/query config "select 42")
~~~

Under the hood, the library will open a new connection from this map, perform a
query and close the connection afterwards. Of course, opening a closing a
connection on each query is much slower that reusing the same one, especially in
a loop. But it gives you more freedom, and sometimes this is also fine in tests.

What's important, you can pass a `Pool` instance into the `query` and `execute`
functions as well. For a pool, the function borrows a connection first, making
it unavailable consumers in other threads. Once the query is done, the
connection is returned to the pool, meaning it's now available for others.

~~~clojure
(def pool (pg/pool {...}))

(pg/query pool "select 42")
~~~

# Supported Sources

At the moment of writing this, the following objects may act as a source:

| Class                         | On borrow logic               | On return logic           |
|-------------------------------|-------------------------------|---------------------------|
| `org.pg.Connection`           | Return itself                 | Nothing (keep it open)    |
| `org.pg.Pool`                 | Borrow one from a pool (lock) | Return to a pool (unlock) |
| `clojure.lang.IPersistentMap` | Open a connection             | Close the connection      |
| `org.pg.Config`               | Open a connection             | Close the connection      |

# Naming

Some `pg.core` functions accept any data source, but some are bound to the
`Connection` object. For example, `query`, `execute`, `copy`, etc accept a
source, but `close-statement` needs a connection because prepared statements are
defined per connection. There is a naming rule for that:

- if the first argument is called `src`, the function accepts a data source (a
  connection, a pool, a clojure map);
- if the first argument is called `conn`, it accepts a connection only. Most
  likely you need to pass a certain connection, for example the one you used for
  a prepared statement or to process notifications.
