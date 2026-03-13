# Parsing DB Column Names

By default, PG2 handles column names as keywords:

~~~clojure
(pg/execute conn "select 1 as foo, 'hello' as bar")

[{:foo 1 :bar "hello"}]
~~~

You can change this behavior either globally (per config) or locally (per
query).

## Kebab-case

For `kebab-keys`, pass the `{:kebab-keys? true}` option into a query or a
config:

~~~clojure
(pg/execute conn "select 1 as just_one" {:kebab-keys? true})

[{:just-one 1}]
~~~

To use such keys in all queries, pass it into a config map:

~~~clojure
(pg/with-connection [conn (assoc config :kebab-keys? true)]
  (pg/execute conn "select 1 as just_one"))

[{:just-one 1}]
~~~

## Custom Functions

Both `query`, `execute` functions and a config map accept a `:fn-key` option
which is a 1-argument function. The function accepts a raw column name as a
string and transforms it as you like:

~~~clojure
;; locally
(pg/execute conn "select 1 as foo" {:fn-key str/upper-case})
[{"FOO" 1}]

;; globally
(pg/with-connection [conn (assoc config :fn-key str/upper-case)]
  (pg/execute conn "select 1 as just_one"))
[{"JUST_ONE" 1}]
~~~

Above, we transformed keys into upper-case strings. To keep an origin string,
pass the standard `clojure.core/identity` function.

## Using Connection URI

Keys transformation can be specified in a connection URI string as well:

~~~text
;; a boolean flag
postgresql://test:test@localhost:%s/test?ssl=false&kebab-keys=true

;; a function reference
postgresql://test:test@localhost:%s/test?ssl=false&fn-key=clojure.string/upper-case
~~~

See a corresponding section for more details: [URI Connection
String](/docs/connection-uri.md).

## The pg.keys Namespace

There is a separate `pg.keys` namespace for functions to transform keys.
