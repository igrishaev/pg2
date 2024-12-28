# HugSQL Support

[hugsql]: https://www.hugsql.org/

The `pg2-hugsql` package brings integration with the [HugSQL library][hugsql].
It creates functions out from SQL files like HugSQL does but these functions use
the PG2 client instead of JDBC. Under the hood, there is a special database
adapter as well as a slight override of protocols to make inner HugSQL stuff
compatible with PG2.

Since the package already depends on core HugSQL functionality, there is no need
to add the latter to dependencies: having `pg2-hugsql` by itself will be enough
(see [Installation](/docs/installation.md)).

## Basic Usage

Let's go through a short demo. Imagine we have a `demo.sql` file with the
following queries:

~~~sql
-- :name create-demo-table :!
create table :i:table (id serial primary key, title text not null);

-- :name insert-into-table :! :n
insert into :i:table (title) values (:title);

-- :name insert-into-table-returning :<!
insert into :i:table (title) values (:title) returning *;

-- :name select-from-table :? :*
select * from :i:table order by id;

-- :name get-by-id :? :1
select * from :i:table where id = :id limit 1;

-- :name get-by-ids :? :*
select * from :i:table where id in (:v*:ids) order by id;

-- :name insert-rows :<!
insert into :i:table (id, title) values :t*:rows returning *;

-- :name update-title-by-id :<!
update :i:table set title = :title where id = :id returning *;

-- :name delete-from-tablee :n
delete from :i:table;
~~~

Prepare a namespace with all the imports:

~~~clojure
(ns pg.demo
  (:require
   [clojure.java.io :as io]
   [pg.hugsql :as hug]
   [pg.core :as pg]))
~~~

To inject functions from the file, pass it into the `pg.hugsql/def-db-fns`
function:

~~~clojure
(hug/def-db-fns (io/file "test/demo.sql"))
~~~

It accepts either a string path to a file, a resource, or a `File`
object. Should there were no exceptions, and the file was correct, the current
namespace will get new functions declared in the file. Let's examine them and
their metadata:

~~~clojure
create-demo-table
#function[pg.demo...]

(-> create-demo-table var meta)

{:doc ""
 :command :!
 :result :raw
 :file "test/demo.sql"
 :line 2
 :arglists ([db] [db params] [db params opt])
 :name create-demo-table
 :ns #namespace[pg.demo]}
~~~

Each newborn function has at most three bodies:

- `[db]`
- `[db params]`
- `[db params opt]`,

where:

- `db` is a source of a connection. It might either a `Connection` object, a
  plain Clojure config map, or a `Pool` object.
- `params` is a map of HugSQL parameters like `{:id 42}`;
- `opt` is a map of `pg/execute` parameters that affect processing the current
  query.

Now that we have functions, let's call them. Establish a connection first:

~~~clojure
(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"})

(def conn
  (jdbc/get-connection config))
~~~

Let's create a table using the `create-demo-table` function:

~~~clojure
(def TABLE "demo123")

(create-demo-table conn {:table TABLE})
{:command "CREATE TABLE"}
~~~

Insert something into the table:

~~~clojure
(insert-into-table conn {:table TABLE
                         :title "hello"})
1
~~~

The `insert-into-table` function has the `:n` flag in the source SQL file. Thus,
it returns the number of rows affected by the command. Above, there was a single
record inserted.

Let's try an expression that inserts something and returns the data:

~~~clojure
(insert-into-table-returning conn
                             {:table TABLE
                              :title "test"})
[{:title "test", :id 2}]
~~~

Now that the table is not empty any longer, let's select from it:

~~~clojure
(select-from-table conn {:table TABLE})

[{:title "hello", :id 1}
 {:title "test", :id 2}]
~~~

The `get-by-id` shortcut fetches a single row by its primary key. It returs
`nil` for a missing key:

~~~clojure
(get-by-id conn {:table TABLE
                 :id 1})
{:title "hello", :id 1}

(get-by-id conn {:table TABLE
                 :id 123})
nil
~~~

Its bulk version called `get-by-ids` relies on the `in (:v*:ids)` HugSQL
syntax. It expands into the following SQL vector: `["... where id in ($1, $2,
... )" 1 2 ...]`

~~~sql
-- :name get-by-ids :? :*
select * from :i:table where id in (:v*:ids) order by id;
~~~

~~~clojure
(get-by-ids conn {:table TABLE
                  :ids [1 2 3]})

;; 3 is missing
[{:title "hello", :id 1}
 {:title "test", :id 2}]
~~~

To insert multiple rows at once, use the `:t*` syntax which is short for "tuple
list". Such a parameter expects a sequence of sequences:

~~~sql
-- :name insert-rows :<!
insert into :i:table (id, title) values :t*:rows returning *;
~~~

~~~clojure
(insert-rows conn {:table TABLE
                   :rows [[10 "test10"]
                          [11 "test11"]
                          [12 "test12"]]})

[{:title "test10", :id 10}
 {:title "test11", :id 11}
 {:title "test12", :id 12}]
~~~

Let's update a single row by its id:

~~~clojure
(update-title-by-id conn {:table TABLE
                          :id 1
                          :title "NEW TITLE"})
[{:title "NEW TITLE", :id 1}]
~~~

Finally, clean up the table:

~~~clojure
(delete-from-table conn {:table TABLE})
~~~

## Passing the Source of a Connection

Above, we've been passing a `Connection` object called `conn` to all
functions. But it can be something else as well: a config map or a pool
object. Here is an example with a map:

~~~clojure
(insert-rows {:host "..." :port ... :user "..."}
             {:table TABLE
              :rows [[10 "test10"]
                     [11 "test11"]
                     [12 "test12"]]})
~~~

Pay attention that, when the first argument is a config map, a Connection object
is established from it, and then it gets closed afterward before exiting a
function. This might break a pipeline if you rely on a state stored in a
connection. A temporary table is a good example. Once you close a connection,
all the temporary tables created within this connection get wiped. Thus, if you
create a temp table in the first function, and select from it using the second
function passing a config map, that won't work: the second function won't know
anything about that table.

The first argument might be a Pool instsance as well:

~~~clojure
(pool/with-pool [pool config]
  (let [item1 (get-by-id pool {:table TABLE :id 10})
        item2 (get-by-id pool {:table TABLE :id 11})]
    {:item1 item1
     :item2 item2}))

{:item1 {:title "test10", :id 10},
 :item2 {:title "test11", :id 11}}
~~~

When the source a pool, each function call borrows a connection from it and
returns it back afterwards. But you cannot be sure that both `get-by-id` calls
share the same connection. A parallel thread may interfere and borrow a
connection used in the first `get-by-id` before the second `get-by-id` call
acquires it. As a result, any pipeline that relies on a shared state across two
subsequent function calls might break.

To ensure the functions share the same connection, use either
`pg/with-connection` or `pool/with-connection` macros:

~~~clojure
(pool/with-pool [pool config]
  (pool/with-connection [conn pool]
    (pg/with-tx [conn]
      (insert-into-table conn {:table TABLE :title "AAA"})
      (insert-into-table conn {:table TABLE :title "BBB"}))))
~~~

Above, there is 100% guarantee that both `insert-into-table` calls share the
same `conn` object borrowed from the pool. It is also wrapped into transaction
which produces the following session:

~~~sql
BEGIN
insert into demo123 (title) values ($1);
  parameters: $1 = 'AAA'
insert into demo123 (title) values ($1);
  parameters: $1 = 'BBB'
COMMIT
~~~

## Passing Options

PG2 supports a lot of options when processing a query. To use them, pass a map
into the third parameter of any function. Above, we override a function that
processes column names. Let it be not the default `keyword` but
`clojure.string/upper-case`:

~~~clojure
(get-by-id conn
           {:table TABLE :id 1}
           {:fn-key str/upper-case})

{"TITLE" "AAA", "ID" 1}
~~~

If you need such keys everywhere, submitting a map into each call might be
inconvenient. The `def-db-fns` function accepts a map of predefined overrides:

~~~clojure
(hug/def-db-fns
  (io/file "test/demo.sql")
  {:fn-key str/upper-case})
~~~

Now, all the generated functions return string column names in upper case by
default:

~~~clojure
(get-by-id config
           {:table TABLE :id 1})

{"TITLE" "AAA", "ID" 1}
~~~

## Defining SQLVec functions

HugSQL allows you to define functions that do not reach the database directly
but only produce a SQL vector. This is a plain Clojure vector where the first
item is a SQL expressions and all the rest items are parameters. For example:

~~~clojure
["select * from users where id = ?" 42]
~~~

Having this vector, you can pass it directly to the JDBC driver. Pay attention
that PG2 mimics Next.JDBC as well and might be used with such vectors. See the
[Next.JDBC API layer](/docs/next-jdbc-layer.md) section for details.

The `pg-honey` package has a function `def-sqlvec-fns` which acts the same: in
the current namespace, it defines functions that produce SQL vectors when being
called. The only difference is, they use numbered dollars for parameters. Here
is our .sql file:

~~~sql
-- :name insert-into-table :! :n
insert into :i:table (title) values (:title);

-- :name select-value-list :? :*
select * from :i:table where id in (:v*:ids) order by id;
~~~

And a short demo:

~~~clojure
(require '[pg.hugsql :as hugsql])

(hugsql/def-sqlvec-fns (io/file "test/pg/test.sql"))

(insert-into-table-sqlvec {:table "table"
                           :title "hello"})
;; ["insert into table (title) values ($1);" "hello"]


(select-value-list-sqlvec {:table "table"
                           :ids [1 2 3]})
;; ["select * from table where id in ($1,$2,$3) order by id;" 1 2 3]
~~~

Each `*-sqlvec` function has three bodies:

- `[]` for cases when there are no HugSQL parameters;
- `[params]` when a SQL expression accepts HugSQL parameters;
- `[params options]` to pass HugSQL parameters and options (which are not used
  at the moment).

When defining -sqlvec functions, override the suffix using the `:fn-suffix`
parameter as follows:

~~~clojure
(hugsql/def-sqlvec-fns (io/file "test/pg/test.sql")
                       {:fn-suffix "-hello"})
~~~

Now all the functions end with `-hello`:

~~~clojure
(insert-into-table-hello {:table "table"
                          :title "hello"}
                         {:some :options})

;; ["insert into table (title) values ($1);" "hello"]
~~~

* * *

For more details, please refer to the official [HugSQL][hugsql] documentation.
