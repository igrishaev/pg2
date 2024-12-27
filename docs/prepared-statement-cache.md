# Prepared Statement Cache

Prepared statements, although thought to bring much performance, might be
inconvenient. The main problem with them is that they're bound to a certain
connection. If one created a prepared statement in connection A, they cannot use
it withing a connection B.

This fact contradicts with real-life applications what use connection
pools. Every time you're about to perform a query, you borrow a connection from
a pool, and there is no any guarantee this is connection you had a second
ago. Thus, running a prepared statement agains a random connection is a bad
idea.

The official JDBC driver for Postgres has an interesting feature that PG2
derives. Every time a query gets executed, a corresponding prepared statement is
not closed afterwards. Instead, it stays open and put in a cache map like `{SQL
-> Statement}`. If you execute the same query (even with other parameters), the
statement is taken from a cache.

This technique greatly simplifies the pipeline. It's not needed to keep one's
eye on prepared statements and their match to a certain connection.

The following session demonstrates caching:


(def config
    {:port 15432
     :user "test"
     :password "test"
     :database "test"}
    )


(def conn (pg/connect config))

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
[]

(pg/execute conn "select $1::int as num" {:params [1]})
[{:num 1}]

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
?

(pg/execute conn "select $1::int as num" {:params [999]})
[{:num 999}]

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
?
