# Unix Domain Sockets

[wiki]: https://en.wikipedia.org/wiki/Unix_domain_socket

When both application and Postgres are running on the same machine, it's
possible to reach Postgres using a [Unix domain socket][wiki]. Technically it's
a special file that, under the hood, serves communication between two processes
without network interaction. Thus, Unix sockets are usually faster than a local
host.

They're available on POSIX-compatible operating systems meaning no luck on
Windows. MacOS supports file sockets.

By default, Postgres trusts any connection that comes though a Unix socket. This
is set in the `pg_hba.conf` config file as follows:

~~~text
# TYPE  DATABASE        USER            ADDRESS                 METHOD
# "local" is for Unix domain socket connections only
local   all             all                                     trust
~~~

The `trust` setting disables any authentication checks during a startup phase of
a connection.

To connect to a file socket, there is a couple of parameters in the connection
config:

- `:unix-socket?`: default is false. When true, PG2 tries to reach a file socket
  rather than a remote host. If you're not on Linux or MacOS (or, khm, Solaris),
  most likely you'll get an exception saying your operating system is not
  supported.

- `:unix-socket-path`: a string, a custom path to a Unix domain socket. When not
  set, PG2 tries to guess the file path (see below). Should your Postgres
  installation store socket files somewhere else, specify the path explicitly
  using this parameter.

## Guessing The Path

When `unix-socket?` is true but `unix-socket-path` is not set, PG2 builds the
path for your. On Linux, it's usually `/var/run/postgresql/.s.PGSQL.5432`, and
on MacOS, it's `/private/tmp/.s.PGSQL.5432`. Pay attention the path includes a
port number. If Postgres runs on port 15432, the filename will be
`.s.PGSQL.15432`. Thus, the algorithm takes the `:port` parameter from the
config map when building a path to a socket.

## Examples

Here is how you connect Postgres using a Unix socket file, assuming it's
location matches PG2 defaults:

~~~clojure
(pg/with-conn [conn {:unix-socket? true
                     :user "test"
                     :database "test"}]
  (pg/query conn "select 1"))
~~~

If Postgres runs on a different port, specify it too:

~~~clojure
(pg/with-conn [conn {:unix-socket? true
                     :port 15432
                     :user "test"
                     :database "test"}]
    (pg/query conn "select 1"))
~~~

Alternatively, specify a full path to a socket file:

~~~clojure
(pg/with-conn [conn {:unix-socket-path "/private/tmp/.s.PGSQL.15432"
                     :user "test"
                     :database "test"}]
    (pg/query conn "select 1"))
~~~

Printing a connection will reflect its Unix socket path:

~~~clojure
(println conn)
;; <PG connection test:test /private/tmp/.s.PGSQL.15432>
~~~

## Performance Gain

When benchmarking Unix sockets versus TCP on localhost, I noticed about 10-15%
performace gain in favour of sockets, namely:

- 140ms vs 160ms on Intel core i5;
- 76ms vs 93ms on ARM M3.

Some people claim that in special cases, Unix sockets benefit a lot. There is
quite an interesting article ["How many TPS can we get from a single Postgres
node?"][linkedin] written by Nikolay Samokhvalov. According to Nikolay, Unix
sockets might bring up to 25% of transaction per second rate, namely 4 million
versus 3 million, which is significant.

![](/media/unix_vs_tcp.png)

[linkedin]: https://www.linkedin.com/pulse/how-many-tps-can-we-get-from-single-postgres-node-nikolay-samokhvalov-yu0rc/

Should any of you have managed to confirm or deny these statements, please
describe you case and it will take its place here.
