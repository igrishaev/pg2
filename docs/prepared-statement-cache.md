# Prepared Statement Cache

Prepared statements, although thought to bring much performance, might be
inconvenient. The main problem with them is, they're bound to a certain
connection. If you prepared a statement in connection A, you cannot use it in a
connection B.

This fact contradicts with real-life applications what rely on connection
pools. Every time you're about to perform a query, you borrow a connection from
a pool, and there is no any guarantee this is connection you had a second
ago. Running a prepared statement against a random connection is a bad idea.

The official JDBC driver for Postgres has an interesting feature that PG2
derives. Every time a query gets executed, a corresponding prepared statement is
kept open afterwards. It stays in a map like `{SQL -> Statement}`. If you
execute the same query again (even with different parameters), the statement is
taken from a cache.

This technique greatly simplifies the pipeline. There is no need to keep one's
eye on prepared statements any more, and check if a statement belongs to a
certain connection.

**Important:** only the `execute` function **does** cache prepared statements;
the standard `query` function **does not**. They might look similar but act
differently under the hood. To read more about the difference, please refer to
[Query and Execute](/docs/query-execute.md) section.

The following session demonstrates how a statement cache works. Let's connect to
a database and check out what prepared statements are there:

~~~clojure
(def config
    {:port 15432
     :user "test"
     :password "test"
     :database "test"})

(def conn (pg/connect config))

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
[]
~~~

We don't have any. But if we `execute` something , a prepared statement will
take place in the `pg_prepared_statements` view:

~~~clojure
(pg/execute conn "select $1::int as num" {:params [1]})
[{:num 1}]

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
[{:statement "select $1::int as num"
  :from_sql false
  :prepare_time ...
  :custom_plans 1
  :name "s139969173240875"
  :generic_plans 0
  :parameter_types "{integer}"}]
~~~

The name of the statement is `s139969173240875`. PG2 uses the `s` prefix for
prepared statements and `p` for portals. The number `139969173240875` is taken
from a `(System/nanoTime)` call. The `from_sql` field and its false value
signals it was produced using the low level Postgres Wire protocol, but not
`PREPARE s1 AS SELECT...` SQL query.

Now if you execute the same query again, even with another parameter, this
prepared statement will be reused:

~~~clojure
(pg/execute conn "select $1::int as num" {:params [999]})
[{:num 999}]
~~~

This step is hidden from a user but may gain performance. Simple queries like
`select $1::int` do not benefit a lot from caching, of course. But complex
queries that involve joins, grouping, etc might take a while when composing a
query plan. In Postgres, building a plan requires fetching statistics about
tables, and this is expensive. But once a statement has been prepared, so has
the plan, and you can easily reuse it.

Imagine someone has manually wiped all the prepared statements with
`DEALLOCATE`:

~~~clojure
(pg/query conn "deallocate all")

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
[]
~~~

The `Connection` object doesn't know about it, and perhaps you expect the next
`execute` invocation to trigger an error. Actually, there will be a negative
response from a server but the `execute` function checks for the error code
before throwing an exception. When it's 2600 (prepared statement not found), the
`execute` function retries the query. Next time, the failed prepared statement
gets removed from the cache and replaced with a new version:

~~~clojure
(pg/execute conn "select $1::int as num" {:params [3]})
[{:num 3}]

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
[{:statement "select $1::int as num"
  :from_sql false
  :prepare_time ...
  :custom_plans 1
  :name "s140141476241250"
  :generic_plans 0
  :parameter_types "{integer}"}]
~~~

Pay attention, the name differs from what we have seen above.

The prepared statement cache uses the origin SQL query as a key. It doens't trim
it, not it performs any reformatting or cleaning. Should any symbol change (even
a leading or a traling space), it is condidered as another expression:

~~~clojure
(pg/execute conn "select 1 as number")
(pg/execute conn " select 1 as number")

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")

[{:statement " select 1 as number"
  :from_sql false
  :prepare_time ...
  :custom_plans 0
  :name "s140219337455583"
  :generic_plans 1
  :parameter_types "{}"}
 {:statement "select 1 as number"
  :from_sql false
  :prepare_time ...
  :custom_plans 0
  :name "s140223610940166"
  :generic_plans 1
  :parameter_types "{}"}]
~~~

There is the `close-cached-statements` function to close all the cached
statements and clean up the cache. It returs the number of statements closed:

~~~clojure
(pg/close-cached-statements conn)
;; 3
~~~

It's unlikely you'll ever need that function though.
