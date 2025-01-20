# URI Connection String

Is it possible to specify most of connection parameters using a URI string. It
reminds Connection URI in JDBC but with minor differences. A URI is useful when
you cannot pass a structured data like JSON/EDN, or in some other rare cases.

A URI might carry a username, a password, a host, a port, and a database
name. Its query string also passes additional parameters. Here is an example of
a URI string:

~~~text
jdbc:postgresql://fred:secret@localhost:5432/test?ssl=true
~~~

Above, the `jdbc:` prefix is just ignored; it's only left for compatibility with
JDBC. When parsed, the URI becomes a map like this:

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

## Query Parameters

This table lists query parameters supported by URI. They act like the standard
configuration options described in the [Connecting to the
Server](/docs/connecting.md) section with rare differences in names
(e.g. "boolean?" names don't have a question mark at the end).

TODO: on parsing

| Parameter                     | Parsed as  | Comment                                                    |
|-------------------------------|------------|------------------------------------------------------------|
| `read-only`                   | bool       | True if make connection always read only                   |
| `so-keep-alive`               | bool       | Enable the standard "keep alive" socket option             |
| `so-tcp-no-delay`             | bool       | Enable the standard "no delay" socket option               |
| `so-timeout`                  | long       | Long value for the standard "timeout" socket option        |
| `so-recv-buf-size`            | long       | Socket receive buffer size                                 |
| `so-send-buf-size`            | long       | Socket send buffer size                                    |
| `binary-encode`               | bool       | Whether to use binary encoding                             |
| `binary-decode`               | bool       | Whether to use binary decoding                             |
| `in-stream-buf-size`          | long       | `BufferedInputStream` default size                         |
| `out-stream-buf-size`         | long       | `BufferedOutputStream` default size                        |
| `ssl`                         | bool       | Whether to use SSL connection                              |
| `ssl-context`                 | reference  | A reference to a custom `SSLContext` object                |
| `fn-notification`             | reference  | A reference to a function handling notifications           |
| `fn-protocol-version`         | reference  | A reference to a function handling protocol mismatch event |
| `fn-notice`                   | reference  | A reference to a function handling notices                 |
| `cancel-timeout-ms`           | long       | A custom timeout duration when cancelling queries          |
| `protocol-version`            | long       | A custom protocol version                                  |
| `object-mapper`               | reference  | A reference to custom JSON `ObjectMapper` instance         |
| `pool-min-size`               | long       | Minimum pool connection size                               |
| `pool-max-size`               | long       | Maximum pool connection size                               |
| `pool-expire-threshold-ms`    | long       | Pool connection expire lifetime                            |
| `pool-borrow-conn-timeout-ms` | long       | How long to wait when borrowing a connection from a pool   |
| `with-pgvector`               | bool       | Parse vector types provided by the `pgvector` extension    |
| `pg-params`                   | nested map | A nested map of Postgres runtime parameters (see below)    |

## JDBC Compatible Parameters

[jdbc-uri]: https://jdbc.postgresql.org/documentation/use/

The following parameters are borrowed from the official [JDBC URI
specification][jdbc-uri]:

| JDBC Parameter        | Acts as                                     |
|-----------------------|---------------------------------------------|
| `readOnly`            | `read-only`                                 |
| `connectTimeout`      | `so-timeout`                                |
| `ApplicationName`     | `pg-params.application_name`                |
| `cancelSignalTimeout` | `cancel-timeout-ms`                         |
| `binaryTransfer`      | Enables `binary-encode` and `binary-decode` |
| `tcpKeepAlive`        | `so-keep-alive`                             |
| `tcpNoDelay`          | `so-tcp-no-delay`                           |
| `protocolVersion`     | `protocol-version`                          |

## Nested PG Params
