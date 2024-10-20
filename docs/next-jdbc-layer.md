# next.jdbc API layer

[next-jdbc]: https://github.com/seancorfield/next-jdbc

`PG2` has a namespace that mimics [next.jdbc][next-jdbc] API. It
doesn't cover 100% of `next.jdbc` features yet most of the functions and macros
are there. It will help you bring `PG2` into project without rewriting
all the database-related code from scratch. Please note that namespace-qualified keys
aren't supported by the wrapper at the moment.

## Obtaining a Connection

In `next.jdbc` all the functions and macros accept something that implements the
`Connectable` protocol. It might be a plain Clojure map, an existing connection,
or a connection pool. The `PG2` wrapper follows this design as well.

Import the namespace and declare a config:

~~~clojure
(require '[pg.jdbc :as jdbc])

(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"})
~~~

Having a config map obtain a connection by passing it into the `get-connection`
function:

~~~clojure
(def conn
  (jdbc/get-connection config))
~~~

Although this approach is a part of the `next.jdbc` design, it's not recommended.
Once you've established a connection you must either close it or if it
was borrowed from a pool return it to the pool. There is a special macro
`on-connection` that covers this logic:

~~~clojure
(jdbc/on-connection [bind source]
  ...)
~~~

- if `source` is a map new connection is spawned and gets closed
afterwards;
- if `source` is pool then connection gets returned to it;
- if `source` is a connection nothing happens on exiting the macro.

~~~clojure
(jdbc/on-connection [conn config]
  (println conn))
~~~

Here's a brief example with a connection pool and a couple of futures. Each future
borrows a connection from a pool, and returns it afterwards.

~~~clojure
(pool/with-pool [pool config]
  (let [f1
        (future
          (jdbc/on-connection [conn1 pool]
            (println
             (jdbc/execute-one! conn1 ["select 'hoho' as message"]))))
        f2
        (future
          (jdbc/on-connection [conn2 pool]
            (println
             (jdbc/execute-one! conn2 ["select 'haha' as message"]))))]
    @f1
    @f2))

;; {{:message hoho}:message haha}
;; two overlapping print statements
~~~

## Executing Queries

Two functions `execute!` and `execute-one!` send queries to the database. Each
of them takes a source, a SQL vector, and a map of options. The SQL vector is a
sequence where the first item is either a string or a prepared statement, and
the rest values are parameters.

~~~clojure
(jdbc/on-connection [conn config]
  (jdbc/execute! conn ["select $1 as num" 42]))
;; [{:num 42}]
~~~

Pay attention that parameters use a dollar sign with a number but not a question
mark.

`execute-one!` function acts like `execute!` but returns the first row
only. Internaly this is done by passing the `{:first? true}` parameter that
enables the `First` reducer.

~~~clojure
(jdbc/on-connection [conn config]
  (jdbc/execute-one! conn ["select $1 as num" 42]))
;; {:num 42}
~~~

To prepare a statement pass a SQL-vector into `prepare` function. The
result will be an instance of the `PreparedStatement` class. To execute a
statement put it into a SQL-vector followed by the parameters:

~~~clojure
(jdbc/on-connection [conn config]
  (let [stmt
        (jdbc/prepare conn
                      ["select $1::int4 + 1 as num"])
        res1
        (jdbc/execute-one! conn [stmt 1])

        res2
        (jdbc/execute-one! conn [stmt 2])]

    [res1 res2]))

;; [{:num 2} {:num 3}]
~~~

Above, the same `stmt` statement is executed twice with different parameters.

More realistic example with inserting data into a table. Let's prepare the table
first:

~~~clojure
(jdbc/execute! config ["create table test2 (id serial primary key, name text not null)"])
~~~

Insert a couple of rows returning the result:

~~~clojure
(jdbc/on-connection [conn config]
  (let [stmt
        (jdbc/prepare conn
                      ["insert into test2 (name) values ($1) returning *"])

        res1
        (jdbc/execute-one! conn [stmt "Ivan"])

        res2
        (jdbc/execute-one! conn [stmt "Huan"])]

    [res1 res2]))

;; [{:name "Ivan", :id 1} {:name "Huan", :id 2}]
~~~

As it was mentioned above, in Postgres a prepared statement is always bound to
a certain connection. Thus, use `prepare` function only inside the
`on-connection` macro to ensure that all the underlying database interaction is
made within the same connection.

## Transactions

The `with-transaction` macro wraps a block of code into a transaction. Before
entering the block, the macro emits the `BEGIN` expression, and `COMMIT`
afterwards, if there was no an exception. Should an exception pop up, the
transaction gets rolled back with `ROLLBACK`, and the exception is re-thrown.

The macro takes a binding symbol which a connection is bound to, a source, an a
map of options. The standard `next.jdbc` transaction options are supported,
namely:

- `:isolation`
- `:read-only`
- `:rollback-only`

Here is an example of inserting a couple of rows in a transaction:

~~~clojure
(jdbc/on-connection [conn config]

  (let [stmt
        (jdbc/prepare conn
                      ["insert into test2 (name) values ($1) returning *"])]

    (jdbc/with-transaction [TX conn {:isolation :serializable
                                     :read-only false
                                     :rollback-only false}]

      (let [res1
            (jdbc/execute-one! conn [stmt "Snip"])

            res2
            (jdbc/execute-one! conn [stmt "Snap"])]

        [res1 res2]))))

;; [{:name "Snip", :id 3} {:name "Snap", :id 4}]
~~~

The Postgres log:

~~~
BEGIN
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
insert into test2 (name) values ($1) returning *
  $1 = 'Snip'
insert into test2 (name) values ($1) returning *
  $1 = 'Snap'
COMMIT
~~~

The `:isolation` parameter might be one of the following:

- `:read-uncommitted`
- `:read-committed`
- `:repeatable-read`
- `:serializable`

For more information on transaction isolation, refer to the official [Postgres
documentation][transaction-iso].

When `read-only` is true, any mutable query will trigger an error response from
Postgres:

~~~clojure
(jdbc/with-transaction [TX config {:read-only true}]
  (jdbc/execute! TX ["delete from test2"]))

;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:207).
;; Server error response: {severity=ERROR, message=cannot execute DELETE in a read-only transaction, verbosity=ERROR}
~~~

When `:rollback-only` is true, the transaction gets rolled back even there was
no an exception. This is useful for tests and experiments:

~~~clojure
(jdbc/with-transaction [TX config {:rollback-only true}]
  (jdbc/execute! TX ["delete from test2"]))
~~~

The logs:

~~~
statement: BEGIN
execute s1/p2: delete from test2
statement: ROLLBACK
~~~

The table still has its data:

~~~clojure
(jdbc/execute! config ["select * from test2"])

;; [{:name "Ivan", :id 1} ...]
~~~

The function `active-tx?` helps to determine if you're in the middle of a
transaction:

~~~clojure
(jdbc/on-connection [conn config]
  (let [res1 (jdbc/active-tx? conn)]
    (jdbc/with-transaction [TX conn]
      (let [res2 (jdbc/active-tx? TX)]
        [res1 res2]))))

;; [false true]
~~~

It returns `true` for transactions that are in the error state as well.

## Keys and Namespaces

The `pg.jdbc` wrapper tries to mimic `next.jdbc` and thus uses `kebab-case-keys`
when building maps:

~~~clojure
(jdbc/on-connection [conn config]
  (jdbc/execute-one! conn ["select 42 as the_answer"]))

;; {:the-answer 42}
~~~

To change that behaviour and use `snake_case_keys`, pass the `{:kebab-keys?
false}` option map:

~~~clojure
(jdbc/on-connection [conn config]
  (jdbc/execute-one! conn
                     ["select 42 as the_answer"]
                     {:kebab-keys? false}))

;; {:the_answer 42}
~~~

By default `next.jdbc` returns namespace-qualified keys where namespaces are table
names (say `:user/profile-id` or `:order/created-at`). Such namespace-qualified keys aren't supported by the wrapper at the moment.
