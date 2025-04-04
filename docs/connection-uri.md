# URI Connection String

Is it possible to specify most of connection parameters using a URI string. It
reminds Connection URI in JDBC but with minor differences. A URI is useful when
you cannot pass a structured data like JSON or EDN.

A URI might carry a username, a password, a host, a port, and a database name. A
query string also passes additional parameters. Here is an example of a URI
string:

~~~text
jdbc:postgresql://fred:secret@localhost:5432/test?ssl=true
~~~

Above, the `jdbc:` prefix is ignored; it is for compatibility with JDBC
only. When parsed, the URI becomes a map like this:

~~~clojure
{:user "fred"
 :password "secret"
 :host "localhost"
 :port 5432
 :database "test"
 :ssl? true}
~~~

Most of the fields can be skipped or overridden with a query string. For
example, both `user` and `password` fields can be specified like this:

~~~text
jdbc:postgresql://localhost:5432/test?ssl=true?user=ivan&password=secret123
~~~

To connect using a URI, pass it under the `:connection-uri` parameter as
follows:

~~~clojure
(def URI "postgresql://test:test@127.0.0.1:5432/test?ssl=false")

(pg/with-conn [conn {:connection-uri URI}]
  (let [res (pg/query conn "select 1 as num")]
    ...))
~~~

The top-level options override values from a URI. For example, it's unsafe to
pass the password in URI. It's better to submit it through a dedicated
`:password` top-level field:

~~~clojure
(def URI "postgresql://ivan@127.0.0.1:5432/test?ssl=false")

(pg/with-conn [conn {:connection-uri URI
                     :password (System/getenv "DB_PASSWORD")}]
  (let [res (pg/query conn "select 1 as num")]
    ...))
~~~

## Query Parameters

This table lists query parameters supported by URI. They act like the standard
configuration options described in the [Connecting to the
Server](/docs/connecting.md) section with some minor differences in names
(e.g. "boolean?" names don't have a question mark at the end).

Before you read, here is a brief description of types and parsing agreements.

- **boolean** values are passed as `true`, `on`, `1`, `yes` for true, and
  `false`, `off`, `0`, `no` for false, for example `read-only=true` or
  `read-only=off`;

- **long** values are passed as a group of numbers: `so-timeout=5000`;

- **reference** values must be fully qualified strings pointing to a certain
  Clojure definition, for example:

  ~~~
  fn-notification=com.acme.server/my-handler
  ~~~

  The `com.acme.server/my-handler` string gets transformed into a symbol first,
  and then passed to the `requiring-resolve` function that returns a Clojure
  variable. It also loads a namepsace if it was not loaded before. Passing a
  string that points to non-existing object will cause an exception.

- **nested** means nested maps, for example: `foo.bar=1&foo.kek=2`. This
  expression becomes `{:foo {:bar 1 :kek 2}}` when parsed. Each parameter is
  split with a dot on a vector of lexems, and each vector is passed into the
  `assoc-in` function in a loop. So far, only one group of parameters rely on
  nesting (see below).

| Parameter                     | Parsed as | Comment                                                                                        |
|-------------------------------|-----------|------------------------------------------------------------------------------------------------|
| `read-only`                   | bool      | True if make connection always read only                                                       |
| `so-keep-alive`               | bool      | Enable the standard "keep alive" socket option                                                 |
| `so-tcp-no-delay`             | bool      | Enable the standard "no delay" socket option                                                   |
| `so-timeout`                  | long      | Long value for the standard "timeout" socket option                                            |
| `so-recv-buf-size`            | long      | Socket receive buffer size                                                                     |
| `so-send-buf-size`            | long      | Socket send buffer size                                                                        |
| `binary-encode`               | bool      | Whether to use binary encoding                                                                 |
| `binary-decode`               | bool      | Whether to use binary decoding                                                                 |
| `in-stream-buf-size`          | long      | `BufferedInputStream` default size                                                             |
| `out-stream-buf-size`         | long      | `BufferedOutputStream` default size                                                            |
| `ssl`                         | bool      | Whether to use SSL connection                                                                  |
| `ssl-context`                 | ref       | A reference to a custom `SSLContext` object                                                    |
| `fn-notification`             | ref       | A reference to a function handling notifications                                               |
| `fn-protocol-version`         | ref       | A reference to a function handling protocol mismatch event                                     |
| `fn-notice`                   | ref       | A reference to a function handling notices                                                     |
| `cancel-timeout-ms`           | long      | A custom timeout duration when cancelling queries                                              |
| `protocol-version`            | long      | A custom protocol version                                                                      |
| `object-mapper`               | ref       | A reference to custom JSON `ObjectMapper` instance                                             |
| `pool-min-size`               | long      | Minimum pool connection size                                                                   |
| `pool-max-size`               | long      | Maximum pool connection size                                                                   |
| `pool-expire-threshold-ms`    | long      | Pool connection expire lifetime                                                                |
| `pool-borrow-conn-timeout-ms` | long      | How long to wait when borrowing a connection from a pool                                       |
| `read-pg-types`               | bool      | Whether to [read Postgres types](/docs/read-pg-types.md) after connection has been established |
| `pg-params`                   | nested    | A nested map of Postgres runtime parameters (see below)                                        |

## JDBC Compatible Parameters

[jdbc-uri]: https://jdbc.postgresql.org/documentation/use/

The following parameters are borrowed from the official [JDBC URI
specification][jdbc-uri] and act like aliases:

| JDBC Parameter        | Alias for                                        |
|-----------------------|--------------------------------------------------|
| `readOnly`            | `read-only`                                      |
| `connectTimeout`      | `so-timeout`                                     |
| `ApplicationName`     | `pg-params.application_name`                     |
| `cancelSignalTimeout` | `cancel-timeout-ms`                              |
| `binaryTransfer`      | Enables both `binary-encode` and `binary-decode` |
| `tcpKeepAlive`        | `so-keep-alive`                                  |
| `tcpNoDelay`          | `so-tcp-no-delay`                                |
| `protocolVersion`     | `protocol-version`                               |

## Postgres Runtime Parameters (pg-params)

The `pg-params` option represents a map of Postgres runtime settings. These
settings apply to the current connection only and do not affect global server
configuration.

[runtime]: https://www.postgresql.org/docs/current/runtime-config.html

Each option should be passes as follows:

~~~
pg-param.<setting1>=<value1>&pg-param.<setting2>=<value2>...
~~~

where

- `settingN` is a name of a setting, e.g. `application_name`, or
  `default_transaction_read_only`, or something else. It should exactly match
  the name of a Postgres runtime parameter, e.g. keep underscores;

- `valueN` is a value of a runtime parameter, e.g. a string or a number, or a
  comma-separated list.

Example:

~~~
pg-param.application_name=my-super-app&pg-param.default_transaction_read_only=off
~~~

For more details, take a look at the [list of Postgres runtime
parameters][runtime] supported.
