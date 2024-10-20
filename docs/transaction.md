# Transactions

To execute one or more queries in transaction wrap them with `begin` and
`commit` as follows:

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

Both rows are inserted in a transaction. Should one of them fail none will
succeed. By checking the database log you'll see the following entries:

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

## `with-tx` macro

`with-tx` handles `BEGIN`, `COMMIT`, and `ROLLBACK` logic.

It opens a transaction, executes it and commits if there was no an exception.
If there was an exception it rolls back the transaction and re-throws it.

The first argument is a connection object:

~~~clojure
(pg/with-tx [conn]
  (pg/execute conn
              "delete from test1 where name like $1"
              {:params ["Test%"]})
  (pg/execute conn
              "insert into test1 (name) values ($1)"
              {:params ["Test3"]}))
~~~

It expands into something like this:

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

`with-tx` accepts several optional parameters that affect a transaction:

| Name               | Type              | Description                                                                  |
|--------------------|-------------------|------------------------------------------------------------------------------|
| `:read-only?`      | Boolean           | When true only read operations are allowed                                   |
| `:rollback?`       | Boolean           | When true the transaction gets rolled back even if there was no an exception |
| `:isolation-level` | Keyword or String | Set isolation level for the current transaction                              |

## Read-only Mode

With `:read-only?` set to `true` only `SELECT` queries will work.

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

With `:rollback?` set to `true` transaction will be rolled back even if it was successful.

This is useful for tests:

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

In this example you'll have all the rows deleted inside `with-tx` but they will be back again after.

## Isolation Level

`:isolation-level` sets isolation level for the current
transaction. The table below shows its possible values:

| `:isolation-level`                        | Postgres level   |
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

The default transaction isolation level depends on the configuration of your database.

[transaction-iso]: https://www.postgresql.org/docs/current/transaction-iso.html

This document doesn't describe the difference between isolation levels. Please
refer to the [official documentation][transaction-iso] for more information.

## Nested Transactions

Nesting one `with-tx` inside another just works and you can easily nest functions which use `with-tx` inside.

`with-tx` omits `BEGIN`/`COMMIT` if connection is in transaction mode already.
