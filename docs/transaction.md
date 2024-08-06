# Transactions

To execute one or more queries in a transaction, wrap them with `begin` and
`commit` functions as follows:

~~~clojure
(pg/begin conn)

(pg/execute conn
            "insert into test1 (name) values ($1)"
            {:params ["Test1"]})

(pg/execute conn
            "insert into test1 (name) values ($1)"
            {:params ["Test2"]})

(pg/commit conn)
~~~

Both rows are inserted in a transaction. Should one of them fail, none will
succeed. By checking the database log, you'll see the following entries:

~~~
statement: BEGIN
execute s23/p24: insert into test1 (name) values ($1)
  parameters: $1 = 'Test1'
execute s25/p26: insert into test1 (name) values ($1)
  parameters: $1 = 'Test2'
statement: COMMIT
~~~

The `rollback` function rolls back the current transaction. The "Test3" entry
will be available during transaction but won't be stored at the end.

~~~clojure
(pg/begin conn)
(pg/execute conn
            "insert into test1 (name) values ($1)"
            {:params ["Test3"]})
(pg/rollback conn)
~~~

## The macro

There is a macro what handles `BEGIN`, `COMMIT`, and `ROLLBACK` logic for
you. The `with-tx` one wraps a block of code. It opens a transaction, executes
the body and, if there was no an exception, commits it. If there was an
exception, the macro rolls back the transaction and re-throws it.

The first argument of the macro is a connection object:

~~~clojure
(pg/with-tx [conn]
  (pg/execute conn
              "delete from test1 where name like $1"
              {:params ["Test%"]})
  (pg/execute conn
              "insert into test1 (name) values ($1)"
              {:params ["Test3"]}))
~~~

The macro expands into something like this:

~~~clojure
(pg/begin conn)
(try
  (let [result (do <body>)]
    (pg/commit conn)
    result)
  (catch Throwable e
    (pg/rollback conn)
    (throw e)))
~~~

The macro accepts several optional parameters that affect a transaction, namely:

| Name               | Type              | Description                                                                  |
|--------------------|-------------------|------------------------------------------------------------------------------|
| `:read-only?`      | Boolean           | When true, only read operations are allowed                                  |
| `:rollback?`       | Boolean           | When true, he transaction gets rolled back even if there was no an exception |
| `:isolation-level` | Keyword or String | Set isolation level for the current transaction                              |

## Read Only

The `:read-only?` parameter set to true restricts all the queries in this
transaction to be read only. Thus, only `SELECT` queries will work. Running
`INSERT`, `UPDATE`, or `DELETE` will cause an exception:

~~~clojure
(pg/with-tx [conn {:read-only? true}]
  (pg/execute conn
              "delete from test1 where name like $1"
              {:params ["Test%"]}))

;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:205).
;; Server error response: {severity=ERROR, code=25006, ... message=cannot execute DELETE in a read-only transaction, verbosity=ERROR}
~~~

## Rolling Back

The `:rollback?` parameter, when set to true, rolls back a transaction even if
it was successful. This is useful for tests:

~~~clojure
(pg/with-tx [conn {:rollback? true}]
  (pg/execute conn
              "delete from test1 where name like $1"
              {:params ["Test%"]}))

;; statement: BEGIN
;; execute s11/p12: delete from test1 where name like $1
;;   parameters: $1 = 'Test%'
;; statement: ROLLBACK
~~~

Above, inside the `with-tx` macro, you'll have all the rows deleted but once you
get back, they will be there again.

## Isolation Level

Finally, the `:isolation-level` parameter sets isolation level for the current
transaction. The table below shows its possible values:

| `:isolation-level` parameter              | Postgres level   |
|-------------------------------------------|------------------|
| `:read-uncommitted`, `"READ UNCOMMITTED"` | READ UNCOMMITTED |
| `:read-committed`, `"READ COMMITTED"`     | READ COMMITTED   |
| `:repeatable-read`, `"REPEATABLE READ"`   | REPEATABLE READ  |
| `:serializable`, `"SERIALIZABLE"`         | SERIALIZABLE     |

Usage:

~~~clojure
(pg/with-tx [conn {:isolation-level :serializable}]
  (pg/execute conn
              "delete from test1 where name like $1"
              {:params ["Test%"]})
  (pg/execute conn
              "insert into test1 (name) values ($1)"
              {:params ["Test3"]}))

;; statement: BEGIN
;; statement: SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
;; execute s33/p34: delete from test1 where name like $1
;;   parameters: $1 = 'Test%'
;; execute s35/p36: insert into test1 (name) values ($1)
;;   parameters: $1 = 'Test3'
;; statement: COMMIT
~~~

The default transation level depends on the settings of your database.

[transaction-iso]: https://www.postgresql.org/docs/current/transaction-iso.html

This document doesn't describe the difference between isolation levels. Please
refer to the [official documentation][transaction-iso] for more information.

## Nested Transactions

Nested transactions happen when you put one `with-tx` inside another, for
example:

~~~clojure
(with-tx [...]          ;; 1
  (do-this ...)
  (with-tx [...]        ;; 2
    (do-that ...)))
~~~

It's not necessary to have them both explicitly; you can easily have two nested
functions `(do-a)` and `(do-b)` each of them driven with the `with-tx` macro:

~~~clojure
(defn do-b [...]
  (with-tx [...]
    (do-something ...)))

(defn do-a [...]
  (with-tx [...]
    (do-b ...)))
~~~

Before version 0.1.17, the library didn't handle nested transactions leaving
them to be controlled by the database. The code above led to the following
sequence of SQL commands:

~~~sql
BEGIN  -- from A
...    -- from A
BEGIN  -- from B
...    -- from B
COMMIT -- from B
...    -- from A
COMMIT -- from A
~~~

Since Postgres doesn't handle nested transactions, here is what going under the
hood. The first `BEGIN` from `do-a` opens a transaction. The second `BEGIN` from
`do-b` gets ignored producing a notice "there is already a transaction".

When it comes to the first `COMMIT`, it commits the current transaction. But
this command comes from `do-b`, not `do-a`! On other words, the order of
`COMMIT` commands is reversed, but the database doesn't know about this. The
first `COMMIT` closes the transaction leaving a part of `do-a` logic being
uncovered. Calling the second `COMMIT` won't do anything since there is no an
active transaction any longer. It will only produce a notice.

Since 0.1.17, PG2 handles this case property. The `with-tx` macro checks if the
connection is in transaction mode by calling the `(pg/in-transaction? ...)`
function. If it's not, the macro works as before: it wraps a block of code with
the `BEGIN/COMMIT/ROLLBACK` commands.

But if the connection is already in transaction, the macro skips `BEGIN/COMMIT`
commands leaving just the body. Thus, having two or more nested `with-tx` macros
will produce the following sequence of SQL expressions:

~~~sql
BEGIN  -- from A
...    -- from A
...    -- from B
...    -- from A
COMMIT -- from A
~~~

With this improvement, you don't need to check manually if underlying code has
its own `with-tx` macro calls.
