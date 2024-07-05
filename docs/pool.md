# Connection Pool

Problem: every time you connect to the database, it takes time to open a socket,
pass authentication pipeline and receive initial data from the server. From the
server's prospective, a new connection means spawning a new process which is
also an expensive operation. In summary, if you open a connection per a query,
your application is about ten times slower than it could be.

Connection pools solve that problem. A pool holds a set of open connections, and
you borrow a connection from a pool. When borrowed, a connection cannot be
shared with somebody else any longer. Once you've done with your work, you
return the connection to the pool, and it's available for other consumers.

PG2 ships a simple and robust connection pool out from the box. This section
covers how to use it.

## A Simple Example

Import the pool namespace as well as the core PG2 namespace as follows:

~~~clojure
(ns demo
  (:require
    [pg.core :as pg]
    [pg.pool :as pool]))
~~~

Here is how you use the pool:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 5432
   :user "test"
   :password "test"
   :database "test"})

(pool/with-pool [pool config]
  (pool/with-connection [conn pool]
    (pg/execute conn "select 1 as one")))
~~~

The `pool/with-pool` macro creates a pool object from the `config` and binds it
to the `pool` symbol. Once you exit the macro, the pool gets closed.

Having a pool object, use it with the `pool/with-connection` macro (there is a
shorter version called `pool/with-conn` as well). This macro borrows a
connection from the pool and binds it to the `conn` symbol. Now you pass the
connection to `pg/execute`, `pg/query` and so on. By exiting the
`with-connection` macro, the connection is returned to the pool.

And this is brightly everything you need to know about the pool! Sections below
describe more about its inner state and behavior.

## Configuration

The pool object accepts the same config that the `Connection` object does (see
the ["Connecting to the server"](/docs/connecting.md) section for the table of
parameters). In addition to these, the fillowing options are accepted:

| Field                          | Type    | Default          | Comment                                                                                                          |
|--------------------------------|---------|------------------|------------------------------------------------------------------------------------------------------------------|
| `:pool-min-size`               | integer | 2                | Minimum number of open connections when initialized                                                              |
| `:pool-max-size`               | integer | 8                | Maximum number of open connections. Cannot be exceeded                                                           |
| `:pool-expire-threshold-ms`    | integer | 300.000 (5 mins) | How soon a connection is treated as expired and will be forcibly closed.                                          |
| `:pool-borrow-conn-timeout-ms` | integer | 15.000 (15 secs) | How long to wait when borrowing a connection while all the connections are busy. By timeout, throw an exception. |

The first option `:pool-min-size` specifies how many connection are opened at
the beginning. Setting too many is not necessary because you never know if you
application will really use all the connections. It's better to start with a
small number and let the pool to grow in time, if needed.

The next option `:pool-max-size` determines the total number of open
connections. When set, it cannot be overridden. If all the connections are busy
and there is still a gap, the pool spawns a new connection and adds it to the
internal queue. But if the `:pool-max-size` value is reached, an exception is
thrown.

The option `:pool-expire-threshold-ms` specifies the number of
milliseconds. When a certain amount of time has passed since the connection's
initialization, it is considered as expired and will be closed by the pool. This
is used to rotate connections and do not let them to live for quite a long time.

The option `:pool-borrow-conn-timeout-ms` prescribes how long to wait when
borrowing a connection from an exhausted pool: a pool where all the connections
are busy and the `:pool-max-size` value has already been reached. At this case,
the only hope that a client who has borrowed a connection in another thread
completes their duties and return the connection before the timeout
occurs. Should there still haven't been any free connections during the
`:pool-borrow-conn-timeout-ms` time window, an exception pops up.

## Pool Methods

stats
replenish-connections?

## Manual Pool Management


pool examples

cases

global pool
manual creation
