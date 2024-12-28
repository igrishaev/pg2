# Query and Execute API

## Query

The `query` function sends a query to the server and returns the
result. Non-data queries return a map with a tag:

~~~clojure
(pg/query conn "create table test1 (id serial primary key, name text)")
{:command "CREATE TABLE"}

(pg/query conn "insert into test1 (name) values ('Ivan'), ('Huan')")
{:inserted 2}
~~~

Data queries return a vector of maps. This behaviour may be changed with
reducers (see below).

~~~clojure
(pg/query conn "select * from test1")
[{:name "Ivan", :id 1} {:name "Huan", :id 2}]
~~~

The SQL string might include several expressions concatenated with a
semicolon. In this case, the result will be a vector of results:

~~~clojure
(pg/query conn "insert into test1 (name) values ('Juan'); select * from test1")

[{:inserted 1}
 [{:name "Ivan", :id 1}
  {:name "Huan", :id 2}
  {:name "Juan", :id 3}]]
~~~

Use this feature wisely; don't try to do lots of things at once.

**Important:** the `query` function doesn't support parameters. You cannot run a
query like these two below or similar:

~~~clojure
(pg/query conn "select * from test1 where id = ?" 1)
;; or
(pg/query conn ["select * from test1 where id = ?" 1])
~~~

**NEVER(!), NEVER(!!), NEVER(!!!) put parameters into a SQL string using `str`,
`format`, or other functions that operate on strings. You will regret it one
day. Use execute with parameters instead.**

## Execute

The `execute` function acts like `query` but has the following peculiarities:

- The SQL string cannot have many expressions concatenated with a
  semicolon. There must be a single expression (although the trailing semicolon
  is allowed).

- It may have parameters. The values for these parameters are passed
  separately. Unlike in JDBC, the parameters use dollar sign with a number, for
  example `$1`, `$2`, etc.

Here is how you can query a row by its primary key:

~~~clojure
(pg/execute conn "select * from test1 where id = $1" {:params [2]})
;; [{:name "Huan", :id 2}]
~~~

The values are passed into the `:params` key; they must be a vector, or a list,
or a lazy sequence. Passing a set is not recommended as it doesn't guarantee the
order of the values.

This is how you insert values into a table using parameters:

~~~clojure
(pg/execute conn
            "insert into test1 (name) values ($1), ($2), ($3)"
            {:params ["Huey" "Dewey" "Louie"]})
;; {:inserted 3}
~~~

Pay attention that the values are always a flat list. Imagine you'd like to
insert rows with explicit ids:

~~~clojure
(pg/execute conn
            "insert into test1 (id, name) values ($1, $2), ($3, $4), ($5, $6)"
            {:params [1001 "Harry" 1002 "Hermione" 1003 "Ron"]})
;; {:inserted 3}
~~~

The `:params` vector consists from flat values but not pairs like `[1001
"Harry"]`. For better readability, make a list of pairs and then `flatten` it:

~~~clojure
(def pairs
  [[1001 "Harry"]
   [1002 "Hermione"]
   [1003 "Ron"]])

(flatten pairs)

;; (1001 "Harry" 1002 "Hermione" 1003 "Ron")
~~~

Since the parameters have explicit numbers, you can reference a certain value
many times. The following query will create three agents Smith with different
ids.

~~~clojure
(pg/execute conn
            "insert into test1 (name) values ($1), ($1), ($1)"
            {:params ["Agent Smith"]})
;; {:inserted 3}
~~~

Both `query` and `execute` functions accept various options that affect data
processing. Find their description in the next section.

**UPD:** in a recent relese, the `execute` function caches prepared statements.
See the [Prepared Statement Cache](/docs/prepared-statement-cache.md) section for
more info.
