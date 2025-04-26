# Connecting to the Server

To connect to a database, define a config map and pass it into the `connect`
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
fields and their values in a table below.

Another way to specify connection parameters is to use a URI string. Below, it
acts as a part of a config map:

~~~clojure
(def URI "postgresql://test:query123@127.0.0.1:5432/test?ssl=false")

(pg/with-conn [conn {:connection-uri URI}]
  (let [res (pg/query conn "select 1 as num")]
    ...))
~~~

Or just pass a URI as is:

~~~clojure
(pg/with-conn [conn URI]
  (let [res (pg/query conn "select 1 as num")]
    ...))
~~~

See the [URI Connection String](/docs/connection-uri.md) section for details.

To close a connection, pass it into the `close` function:

~~~clojure
(pg/close conn)
~~~

Once closed, a connection cannot be reopened or used afterwards.

To close a connection automatically, use either `with-connection` or `with-open`
macro. The `with-connection` macro takes a binding symbol and a config map; the
connection gets bound to the binding symbol while executing the body:

~~~clojure
(pg/with-connection [conn config]
  (pg/query conn "select 1 as one"))
~~~

There is a shorter version of this macro called `with-conn`:

~~~clojure
(pg/with-conn [conn config]
  (pg/query conn "select 1 as one"))
~~~

The standard `with-open` macro calls the `(.close connection)` method on exit:

~~~clojure
(with-open [conn (pg/connect config)]
  (pg/query conn "select 1 as one"))
~~~

Please avoid cases when you close connections manually. Use one of these two
macros shown above.

Use `:pg-params` field to specify connection-specific Postgres parameters. These
are "TimeZone", "application_name", "DateStyle" and more. Both keys and values
are plain strings:

~~~clojure
(def config+
  (assoc config
         :pg-params
         {"application_name" "Clojure"
          "DateStyle" "ISO, MDY"}))

(def conn
  (pg/connect config+))
~~~

## Connection parameters

The following table describes all the possible connection options with the
possible values and semantics. Only the two first options are requred. All the
rest have predefined values.

| Field                  | Type         | Default            | Comment                                                                             |
|------------------------|--------------|--------------------|-------------------------------------------------------------------------------------|
| `:user`                | string       | **required**       | the name of the DB user                                                             |
| `:database`            | string       | **required**       | the name of the database                                                            |
| `:connection-uri`      | string       | nil                | A [URI string](/docs/connection-uri.md) with a user, database, and other parameters |
| `:host`                | string       | 127.0.0.1          | IP or hostname                                                                      |
| `:port`                | integer      | 5432               | port number                                                                         |
| `:password`            | string       | ""                 | DB user password                                                                    |
| `:pg-params`           | map          | {}                 | A map of session params like {string string}                                        |
| `:binary-encode?`      | bool         | false              | Whether to use binary data encoding                                                 |
| `:binary-decode?`      | bool         | false              | Whether to use binary data decoding                                                 |
| `:read-only?`          | bool         | false              | Whether to initiate this connection in READ ONLY mode (see below)                   |
| `:in-stream-buf-size`  | integer      | 0xFFFF             | Size of the input buffered socket stream                                            |
| `:out-stream-buf-size` | integer      | 0xFFFF             | Size of the output buffered socket stream                                           |
| `:fn-notification`     | 1-arg fn     | logging fn         | A function to handle notifications                                                  |
| `:fn-protocol-version` | 1-arg fn     | logging fn         | A function to handle negotiation version protocol event                             |
| `:fn-notice`           | 1-arg fn     | logging fn         | A function to handle notices                                                        |
| `:ssl?`                | bool         | false              | Whether to use an [SSL connection](/docs/ssl.md)                                    |
| `:use-ssl?`            | bool         | false              | **Deprecated:** an outdated version of `:ssl?`                                      |
| `:ssl-validation`      | mixed        | nil                | How (and if) to validate SSL certificates                                           |
| `:ssl-context`         | SSLContext   | nil                | An custom instance of `SSLContext` class to wrap a socket                           |
| `:unix-socket?`        | bool         | false              | Whether to connect to a [Unix domain socket](/docs/unix-socket.md)                  |
| `:unix-socket-path`    | string       | null               | A custom path to Unix domain socket                                                 |
| `:so-keep-alive?`      | bool         | true               | Socket KeepAlive value                                                              |
| `:so-tcp-no-delay?`    | bool         | true               | Socket TcpNoDelay value                                                             |
| `:so-timeout`          | integer      | 15.000             | Socket timeout value, in ms                                                         |
| `:so-recv-buf-size`    | integer      | 0xFFFF             | Socket receive buffer size                                                          |
| `:so-send-buf-size`    | integer      | 0xFFFF             | Socket send buffer size                                                             |
| `:cancel-timeout-ms`   | integer      | 5.000              | Default value for the `with-timeout` macro, in ms                                   |
| `:protocol-version`    | integer      | 196608             | Postgres protocol version                                                           |
| `:object-mapper`       | ObjectMapper | JSON.defaultMapper | An instance of ObjectMapper for custom JSON processing (see the "JSON" section)     |
| `:ps-cache?`           | bool         | true               | Whether to use [Prepared Statement Cache](docs/prepared-statement-cache.md)         |

### Parameter notes

#### Read Only Mode

The `:read-only?` connection parameter does two things under the hood:

1. It appends the `default_transaction_read_only` parameter to the startup
   message set to `on`. Thus, any transaction gets started on `READ ONLY` mode.

2. It prevents the `:read-only?` flag from overriding in the `with-transaction`
   macro. Say, even if the macro is called like this:

~~~clojure
(pg/with-transaction [tx conn {:read-only? false}] ;; try to mute the global :read-only? flag
  (pg/query tx "delete from students"))
~~~

The transaction will be in `READ ONLY` mode anyway.
