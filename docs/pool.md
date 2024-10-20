# Connection Pool

There's a problem that every time you connect to the database, it takes time to open a socket,
pass authentication pipeline and receive initial data from the server. From the
server's prospective every new connection spawns a new process which is also an
expensive operation. If you open one connection per query your application is
about ten times slower than it could be.

Connection pools solve that problem. The pool holds a set of connections opened in
advance and you *borrow* them from the pool. When borrowed, the connection can't
be shared with somebody else any longer. Once you are done with your work you
return the connection to the pool and it's available for other consumers.

`PG2` ships a simple and robust connection pool out of the box.
covers how to use it.

## A Simple Example

Import both core and pool namespaces as follows:

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

The `pool/with-pool` macro creates a pool object from the `config` map and binds
it to the `pool` symbol. Once you exit the macro the pool gets closed.

`with-pool` macro can be easily replaced with `with-open` macro and
`pool` function that creates a pool instance. By exit the macro calls
`.close` method of an opened object, which closes the pool.

~~~clojure
(with-open [pool (pool/pool config)]
  (pool/with-conn [conn pool]
    (pg/execute conn "select 1 as one")))
~~~

Having a pool object use it with `pool/with-connection` macro (there is a
shorter version `pool/with-conn` as well). This macro borrows a connection from
the pool and binds it to the `conn` symbol. Now you pass the connection to
`pg/execute`, `pg/query`, etc., the connection is returned to the pool afterwards.

And this is briefly everything you need to know about pools. Sections below
describe more about its inner state and behaviour.

## Configuration

The pool object accepts the same config the `Connection` object does (see
[Connecting to the server](/docs/connecting.md) for
parameters). In addition the fillowing options are accepted:

| Field                          | Type    | Default            | Comment                                                                                                              |
|--------------------------------|---------|--------------------|----------------------------------------------------------------------------------------------------------------------|
| `:pool-min-size`               | integer | `2`                | Minimum number of open connections when initialized.                                                                 |
| `:pool-max-size`               | integer | `8`                | Maximum number of open connections. Cannot be exceeded.                                                              |
| `:pool-expire-threshold-ms`    | integer | `300000` (5 mins)  | How soon a connection is treated as expired and will be forcibly closed.                                             |
| `:pool-borrow-conn-timeout-ms` | integer | `15000` (15 secs)  | How long to wait when borrowing a connection while all the connections are busy. By timeout, an exception is thrown. |

`:pool-min-size` specifies how many connections are opened at
the beginning. Setting too many is not necessary because you never know if you
application will really use all of them. It's better to start with a small
number and let the pool to grow in time if needed.

`:pool-max-size` determines the total number of open
connections. When set it can't be overridden. If all the connections are busy
and there is still a gap the pool spawns a new connection and adds it to the
internal queue. But if the `:pool-max-size` value is reached an exception is
thrown.

`:pool-expire-threshold-ms` specifies the number of
milliseconds. When a certain amount of time has passed since the connection's
initialization it is considered expired and will be closed by the pool. This is
used to rotate connections and prevent them from living for too long.

`:pool-borrow-conn-timeout-ms` prescribes how long to wait when
borrowing a connection from an exhausted pool—a pool where all the connections
are taken and the `:pool-max-size` value is reached. In this case the only hope
is that other client will return its connection before
timeout happens. If no free connection appears during the
`:pool-borrow-conn-timeout-ms` time window an exception pops up.

## Pool Metrics

The `stats` function returns info about free and used connections:

~~~clojure
(pool/with-pool [pool config]

  (pool/stats pool)
  ;; {:free 1 :used 0}

  (pool/with-connection [conn pool]
    (pool/stats pool)
    ;; {:free 0 :used 1}
  ))
~~~

It might be used to send metrics to Grafana, CloudWatch, etc.

## Manual Pool Management

[component]: https://github.com/stuartsierra/component
[integrant]: https://github.com/weavejester/integrant

The following functions help you manage a connection pool manually, for example
when it's wrapped into a component (see [Component][component] and
[Integrant][integrant] libraries).

The `pool` function creates a pool:

~~~clojure
(def POOL (pool/pool config))
~~~

`used-count` and `free-count` functions return total numbers of busy and
free connections, respectively:

~~~clojure
(pool/free-count POOL)
;; 2

(pool/used-count POOL)
;; 0
~~~

`pool?` predicate ensures it's a `Pool` instance indeed:

~~~clojure
(pool/pool? POOL)
;; true
~~~

## Closing

`close` function shuts down a pool instance. On shutdown all the free
connections get closed first. Then the pool closes busy connections that were
borrowed. This might lead to failures in other threads so it's worth waiting
until the pool has zero busy connections.

~~~clojure
(pool/close POOL)
;; nil
~~~

`closed?` predicate ensures that pool has already been closed:

~~~clojure
(pool/closed? POOL)
;; true
~~~

## Borrow Logic in Detail

When getting a connection from a pool the following conditions are taken into
account:

- if the pool is closed an exception is thrown;
- if there are free connections available the pool returns one of them;
- if a connection is expired (was created long time ago) it's closed and the pool
  performs another attempt;
- if there aren't free connections but the max number of used connections has not
  been reached yet, the pool spawns a new connection;
- if the number of used connections is reached the pool waits for
  `:pool-borrow-conn-timeout-ms` amount of milliseconds hoping that someone
  releases a connection in the background;
- by timeout (when nobody did) the pool throws an exception.


## Returning Logic in Detail

When you return a connection to the pool the following cases might come into
play:

- if the connection is in error state then transaction is rolled back and the
  connection is closed;
- if the connection is in transaction mode, it is rolled back and the
  connection is marked as free again;
- if it was already closed, the pool just removes it from used connections. It
  won't be added into the free queue;
- if the pool is closed, the connection is removed from used connections;
- when none of the above conditions is met, the connection is removed from used ones and
  becomes available for other consumers again.
