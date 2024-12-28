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

**Important:** only `execute` function caches prepared statements; the standard
`query` function does not. They might look similar but act differently under the
hood. To read more about the difference, please refer to [Query and
Execute](/docs/query-execute.md) section.

The following session demonstrates how cache does work. Let's connect to a
database and, while the connection is fresh, check out what prepared statements
we have:

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

We don't have any. But as soon as we execute a first query, its prepared
statement takes place in the `pg_prepared_statements` view:


~~~clojure
(pg/execute conn "select $1::int as num" {:params [1]})
[{:num 1}]

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
[{:statement "select $1::int as num"
  :from_sql false
  :prepare_time ...
  :custom_plans 1
  :name "s1"
  :generic_plans 0
  :parameter_types "{integer}"}]
~~~

The system name is `s1`. The `from_sql` false value signals it was produced low
level Postgres Wire protocol, but not `PREPARE s1 AS SELECT...` SQL query.

Now if you execute the same query again, even with a different parameter, the
prepared statement `s1` will be reused:

~~~clojure
(pg/execute conn "select $1::int as num" {:params [999]})
[{:num 999}]
~~~

This step is hidden from a user but may gain performance. Simple queries like
`select $1::int` do not benefit a lot from caching, of course. But complex
queries that involve joins, grouping, etc might take a while when composing a
query plan. Building this plan every time you `execute` the same query is
ineffective.

Imagine someone has cleaned up all the prepared statements with `DEALLOCATE`:

~~~clojure
(pg/query conn "deallocate all")

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
[]
~~~

The `Connection` object doesn't know about it, and perhaps you expect the next
`execute` invocation will end with an error. Actually, it will but the `execute`
function checks for the error code received. When it's 2600 (prepared statement
not found), it will retry the query. This time, the failed prepared statement is
removed from the cache and replaced with its new version:

~~~clojure
(pg/execute conn "select $1::int as num" {:params [3]})
[{:num 3}]

(pg/query conn "select * from pg_prepared_statements order by prepare_time asc")
[{:statement "select $1::int as num"
  :from_sql false
  :prepare_time ...
  :custom_plans 1
  :name "s5"
  :generic_plans 0
  :parameter_types "{integer}"}]
~~~

Pay attention, now it's `s5` but not `s1`.

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
  :name "s16"
  :generic_plans 1
  :parameter_types "{}"}
 {:statement "select 1 as number"
  :from_sql false
  :prepare_time ...
  :custom_plans 0
  :name "s19"
  :generic_plans 1
  :parameter_types "{}"}]
~~~

Use the `close-cached-statements` function to close all the cached statements
and clean up the cache. It returs the number of statements closed:

~~~clojure
(pg/close-cached-statements conn)
;; 3
~~~

It's unlikely you'll ever need that function though.
