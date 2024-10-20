# Connecting to the server

Call `connect` with the config map:

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

`conn` is an instance of the `org.pg.Connection` class.

`:host`, `:port`, and `:password` have default values and
can be skipped (password is an empty string by default). Only `:user`
and `:database` are required. See the list of possible
options below.

To close the connection call `close` with it:

~~~clojure
(pg/close conn)
~~~

You can't open or use this connection again afterwards.

To close the connection automatically use either `with-connection` or
`clojure.core/with-open` macros. `with-connection` takes a binding symbol and a
config map; the connection gets bound to the binding symbol while the body is
executed:

~~~clojure
(pg/with-connection [conn config]
  (pg/query conn "select 1 as one"))
~~~

There is a shorter version of this macro called `with-conn`:

~~~clojure
(pg/with-conn [conn config]
  (pg/query conn "select 1 as one"))
~~~

`clojure.core/with-open` calls `(.close connection)` method on exit:

~~~clojure
(with-open [conn (pg/connect config)]
  (pg/query conn "select 1 as one"))
~~~

It's recommended to use one of these macros instead of closing connections manually.

Use `:pg-params` field to specify connection-specific Postgres parameters. These
are "TimeZone", "application_name", "DateStyle", etc. Both keys and values
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

## Connection Parameters

The following table describes all possible connection options.
Only `:user` and `:database` are requred, others have default values.

| Field                  | Type         | Default              | Comment                                                                                   |
|------------------------|--------------|----------------------|-------------------------------------------------------------------------------------------|
| `:user`                | string       | **required**         | DB user name                                                                              |
| `:database`            | string       | **required**         | database name                                                                             |
| `:host`                | string       | `"127.0.0.1"`        | IP or hostname                                                                            |
| `:port`                | integer      | `5432`               | port number                                                                               |
| `:password`            | string       | `""`                 | DB user password                                                                          |
| `:pg-params`           | map          | `{}`                 | Map of session params like {string string}                                                |
| `:binary-encode?`      | bool         | `false`              | Whether to use binary data encoding                                                       |
| `:binary-decode?`      | bool         | `false`              | Whether to use binary data decoding                                                       |
| `:read-only?`          | bool         | `false`              | Whether to initiate this connection in READ ONLY mode (see below)                         |
| `:in-stream-buf-size`  | integer      | `0xFFFF`             | Size of the input buffered socket stream                                                  |
| `:out-stream-buf-size` | integer      | `0xFFFF`             | Size of the output buffered socket stream                                                 |
| `:fn-notification`     | 1-arg fn     | logging fn           | Function to handle notifications                                                          |
| `:fn-protocol-version` | 1-arg fn     | logging fn           | Function to handle negotiation version protocol event                                     |
| `:fn-notice`           | 1-arg fn     | logging fn           | Function to handle notices                                                                |
| `:use-ssl?`            | bool         | `false`              | Whether to use SSL connection                                                             |
| `:ssl-context`         | SSLContext   | `nil`                | Custom instance of `SSLContext` class to wrap a socket                                    |
| `:so-keep-alive?`      | bool         | `true`               | Socket KeepAlive value                                                                    |
| `:so-tcp-no-delay?`    | bool         | `true`               | Socket TcpNoDelay value                                                                   |
| `:so-timeout`          | integer      | `15.000`             | Socket timeout value (in ms)                                                              |
| `:so-recv-buf-size`    | integer      | `0xFFFF`             | Socket receive buffer size                                                                |
| `:so-send-buf-size`    | integer      | `0xFFFF`             | Socket send buffer size                                                                   |
| `:cancel-timeout-ms`   | integer      | `5000`               | Default value for the `with-timeout` macro (in ms)                                        |
| `:protocol-version`    | integer      | `196608`             | Postgres protocol version                                                                 |
| `:object-mapper`       | ObjectMapper | `JSON.defaultMapper` | Instance of `ObjectMapper` for custom JSON processing (see [JSON support](/docs/json.md)) |

### read-only mode

If you set `:read-only? true` _every transaction_ will be started in `READ ONLY` mode, even if `:read-only? false` is passed to `with-tx`.
