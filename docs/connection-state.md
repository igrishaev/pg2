# Connection state

There are some function to track the connection state. In Postgres, the state of
a connection might be one of these:

- **idle** when it's ready for a query;

- **in transaction** when a transaction has already been started but not
  committed yet;

- **error** when transaction has failed but hasn't been rolled back yet.

The `status` function returns either `:I`, `:T`, or `:E` keywords depending on
where you are at the moment. For each state, there is a corresponding predicate
that returns either true of false.

At the beginning, the connection is idle:

~~~clojure
(pg/status conn)
:I

(pg/idle? conn)
true
~~~

Open a transaction and check out the state:

~~~clojure
(pg/begin conn)

(pg/status conn)
:T

(pg/in-transaction? conn)
true
~~~

Now send a broken query to the server. You'll get an exception describing the
error occurred on the server:

~~~clojure
(pg/query conn "selekt dunno")

;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:205).
;; Server error response: {severity=ERROR, code=42601, ...,  message=syntax error at or near "selekt", verbosity=ERROR}
~~~

The connection is in the error state now:

~~~clojure
(pg/status conn)
:E

(pg/tx-error? conn)
true
~~~

When state is error, the connection doesn't accept any new queries. Each of them
will be rejected with a message saying that the connection is in the error
state. To recover from an error, rollback the current transaction:

~~~clojure
(pg/rollback conn)

(pg/idle? conn)
true
~~~

Now it's ready for new queries again.
