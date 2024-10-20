# HoneySQL Integration

[honeysql]: https://github.com/seancorfield/honeysql

The `pg-honey` package (see [Installation](/docs/installation.md)) allows you to
call `query`/`execute` with [HoneySQL][honeysql] maps rather than string SQL expressions.

The package also provides several helpers for common actions like getting
row(s) by id, inserting/updating row(s) with maps of values, etc.

For a demo let's import the package and create a table with some rows:

~~~clojure
(require '[pg.honey :as pgh])

(def config
  {:host "127.0.0.1"
   :port 10140
   :user "test"
   :password "test"
   :dbname "test"})

(def conn
  (pg/connect config))

(pg/query conn "create table test003 (
  id integer not null,
  name text not null,
  active boolean not null default true
)")

(pg/query conn "insert into test003 (id, name, active)
  values
  (1, 'Ivan', true),
  (2, 'Huan', false),
  (3, 'Juan', true)")
~~~

## `get-by-id`

Fetches single row by primary key which is `:id` by default:

~~~clojure
(pgh/get-by-id conn :test003 1)
;; {:name "Ivan", :active true, :id 1}
~~~

*Here and below: pass a `Connection` object as the first argument but it could
be a plain config map or a `Pool` instance as well.*

You can specify primary key and column names you're interested in:

~~~clojure
(pgh/get-by-id conn
               :test003
               1
               {:pk [:raw "test003.id"]
                :fields [:id :name]})

;; {:name "Ivan", :id 1}

;; SELECT id, name FROM test003 WHERE test003.id = $1 LIMIT $2
;; parameters: $1 = '1', $2 = '1'
~~~

## `get-by-ids`

Fetches multiple rows by primary keys using `IN` operator.

In addition to options that `get-by-id` has you can specify the ordering:

~~~clojure
(pgh/get-by-ids conn
                :test003
                [1 3 999]
                {:pk [:raw "test003.id"]
                 :fields [:id :name]
                 :order-by [[:id :desc]]})

[{:name "Juan", :id 3}
 {:name "Ivan", :id 1}]

;; SELECT id, name FROM test003 WHERE test003.id IN ($1, $2, $3) ORDER BY id DESC
;; parameters: $1 = '1', $2 = '3', $3 = '999'
~~~

Passing many IDs at once is not recommended. Either pass them by chunks or
create a temporary table, `COPY IN` ids into it, and `INNER JOIN` with the main
table.

## `delete`

Deletes rows from table. By default all rows are
deleted without filtering and they are returned:

~~~clojure
(pgh/delete conn :test003)

[{:name "Ivan", :active true, :id 1}
 {:name "Huan", :active false, :id 2}
 {:name "Juan", :active true, :id 3}]
~~~

You can specify `:where` clause and column names of the result:

~~~clojure
(pgh/delete conn
            :test003
            {:where [:and
                     [:= :id 3]
                     [:= :active true]]
             :returning [:*]})

[{:name "Juan", :active true, :id 3}]
~~~

With `:returning` set to `nil` no rows are returned.

## `insert`

Inserts row(s) into table.

To observe all the features of `insert` let's create a separate table:

~~~clojure
(pg/query conn "create table test004 (
  id serial primary key,
  name text not null,
  active boolean not null default true
)")
~~~

`insert` accepts a collection of maps representing rows:

~~~clojure
(pgh/insert conn
            :test004
            [{:name "Foo" :active false}
             {:name "Bar" :active true}]
            {:returning [:id :name]})

[{:name "Foo", :id 1}
 {:name "Bar", :id 2}]
~~~

It also accepts options to produce the `ON CONFLICT ... DO ...`  clause known as
`UPSERT`. The following query tries to insert two rows with existing primary
keys. Should they exist the query updates the names of the corresponding rows:

~~~clojure
(pgh/insert conn
            :test004
            [{:id 1 :name "Snip"}
             {:id 2 :name "Snap"}]
            {:on-conflict [:id]
             :do-update-set [:name]
             :returning [:id :name]})
~~~

The resulting query:

~~~sql
INSERT INTO test004 (id, name) VALUES ($1, $2), ($3, $4)
  ON CONFLICT (id)
  DO UPDATE SET name = EXCLUDED.name
  RETURNING id, name
parameters: $1 = '1', $2 = 'Snip', $3 = '2', $4 = 'Snap'
~~~

## `insert-one`

Inserts one row into table. Acts like `insert` but accepts and returns a single map. It supports `:returning` and `:on-conflict` clauses as well:

~~~clojure
(pgh/insert-one conn
                :test004
                {:id 2 :name "Alter Ego" :active true}
                {:on-conflict [:id]
                 :do-update-set [:name :active]
                 :returning [:*]})

{:name "Alter Ego", :active true, :id 2}
~~~

The resulting query:

~~~sql
INSERT INTO test004 (id, name, active) VALUES ($1, $2, TRUE)
  ON CONFLICT (id)
  DO UPDATE SET name = EXCLUDED.name, active = EXCLUDED.active
  RETURNING *
parameters: $1 = '2', $2 = 'Alter Ego'
~~~

### Inserting JSON

When inserting JSON you have to wrap the value with `[:lift ...]` or pass it via `:params` as below:

~~~clojure
(pgh/insert-one conn
                :test_json
                {:data [:lift {:another {:a {:value [1 2 3]}}}]})

{:id 2, :data {:another {:json {:value [1 2 3]}}}}

(pgh/insert-one conn
                :test_json
                {:data [:param :data]}
                {:honey {:params {:data {:another {:a {:value [1 2 3]}}}}}})
~~~


Without `[:lift ...]` tag `HoneySQL` will treat the value as nested SQL map
and try to render it as string (which will fail of course or lead to SQL
injection).

## `update`

Updates rows in table. By default it doesn't do any
filtering and updates all rows. The following query sets the
boolean `active` value for all rows:

~~~clojure
(pgh/update conn
            :test003
            {:active true})

[{:name "Ivan", :active true, :id 1}
 {:name "Huan", :active true, :id 2}
 {:name "Juan", :active true, :id 3}]
~~~

`:where` determines conditions for update. You can also specify
columns to return:

~~~clojure
(pgh/update conn
            :test003
            {:active false}
            {:where [:= :name "Ivan"]
             :returning [:id]})

[{:id 1}]
~~~

What's great about `update` is that you can use complex expressions like
increasing counters, negation, etc. Below we alter primary key by
adding 100 to it, negate `active` column, and change `name` column with
dull concatenation:

~~~clojure
(pgh/update conn
            :test003
            {:id [:+ :id 100]
             :active [:not :active]
             :name [:raw "name || name"]}
            {:where [:= :name "Ivan"]
             :returning [:id :active]})

[{:active true, :id 101}]
~~~

Which produces the following query:

~~~sql
UPDATE test003
  SET
    id = id + $1,
    active = NOT active,
    name = name || name
  WHERE name = $2
  RETURNING id, active
parameters: $1 = '100', $2 = 'Ivan'
~~~

## `find`

Finds rows in table by column-value pairs. All pairs are joined using the `AND` operator:

~~~clojure
(pgh/find conn :test003 {:active true})

[{:name "Ivan", :active true, :id 1}
 {:name "Juan", :active true, :id 3}]
~~~

Find by two conditions:

~~~clojure
(pgh/find conn :test003 {:active true
                         :name "Juan"})

[{:name "Juan", :active true, :id 3}]

;; SELECT * FROM test003 WHERE (active = TRUE) AND (name = $1)
;; parameters: $1 = 'Juan'
~~~

`find` accepts additional options for `LIMIT`, `OFFSET`, and `ORDER BY`
clauses:

~~~clojure
(pgh/find conn
          :test003
          {:active true}
          {:fields [:id :name]
           :limit 10
           :offset 1
           :order-by [[:id :desc]]
           :fn-key identity})

[{"id" 1, "name" "Ivan"}]

;; SELECT id, name FROM test003
;;   WHERE (active = TRUE)
;;   ORDER BY id DESC
;;   LIMIT $1
;;   OFFSET $2
;; parameters: $1 = '10', $2 = '1'
~~~

## `find-first`

Finds one row in table by column-value pairs.

Acts the same as `find` but returns single row or `nil`. Internally it adds `LIMIT 1`.

~~~clojure
(pgh/find-first conn :test003
                {:active true}
                {:fields [:id :name]
                 :offset 1
                 :order-by [[:id :desc]]
                 :fn-key identity})

{"id" 1, "name" "Ivan"}
~~~

## `prepare`

Makes prepared statement from `HoneySQL` map:

~~~clojure
(def stmt
  (pgh/prepare conn {:select [:*]
                     :from :test003
                     :where [:= :id 0]}))

;; <Prepared statement, name: s37, param(s): 1, OIDs: [INT8], SQL: SELECT * FROM test003 WHERE id = $1>
~~~

Above, the zero value is a placeholder for an integer parameter.

Now that the statement is prepared execute it with the id:

~~~clojure
(pg/execute-statement conn stmt {:params [3]
                                 :first? true})

{:name "Juan", :active true, :id 3}
~~~

Alternatively use `[:raw ...]` syntax to specify a parameter with a dollar
sign:

~~~clojure
(def stmt
  (pgh/prepare conn {:select [:*]
                     :from :test003
                     :where [:raw "id = $1"]}))

(pg/execute-statement conn stmt {:params [1]
                                 :first? true})

{:name "Ivan", :active true, :id 1}
~~~

## `query`/`execute`

There are two general functions: `query` and `execute`. Each of them
accepts an arbitrary `HoneySQL` map and performs either `Query` or `Execute`
request to the server.

Both `query` and `execute` accept not only `SELECT` but literally anything:
inserting, updating, creating a table, an index, etc. You can build
combinations like `INSERT ... FROM SELECT` or `UPDATE ... FROM DELETE` to
perform complex logic in a single query.

### `query`

Runs given `HoneySQL` map WITH NO parameters.

Pay attention that `HoneySQL` map can't have parameters when using `query`.
This is a limitation of the `Query` command. The following query
will lead to error response:

~~~clojure
(pgh/query conn
           {:select [:id]
            :from :test003
            :where [:= :name "Ivan"]
            :order-by [:id]})

;; Execution error (PGErrorResponse) at org.pg.Accum/maybeThrowError (Accum.java:207).
;; Server error response: {severity=ERROR, ... message=there is no parameter $1, verbosity=ERROR}
~~~

Instead use either `[:raw ...]` syntax or `{:inline true}` option:

~~~clojure
(pgh/query conn
           {:select [:id]
            :from :test003
            :where [:raw "name = 'Ivan'"] ;; raw (as is)
            :order-by [:id]})

[{:id 1}]

;; OR

(pgh/query conn
           {:select [:id]
            :from :test003
            :where [:= :name "Ivan"]
            :order-by [:id]}
           {:honey {:inline true}}) ;; inline values

[{:id 1}]

;; SELECT id FROM test003 WHERE name = 'Ivan' ORDER BY id ASC
~~~

### `execute`

Executes given `HoneySQL` map with parameters:

~~~clojure
(pgh/execute conn
               {:select [:id :name]
                :from :test003
                :where [:= :name "Ivan"]
                :order-by [:id]})

[{:name "Ivan", :id 1}]
~~~

## `HoneySQL` options

Any `HoneySQL`-specific parameter might be passed through `:honey` map in
options. Below we pass `:params` map to use `[:param ...]`
syntax. Also we produce a pretty-formatted SQL for better logs:

~~~clojure
(pgh/execute conn
             {:select [:id :name]
              :from :test003
              :where [:= :name [:param :name]]
              :order-by [:id]}
             {:honey {:pretty true
                      :params {:name "Ivan"}}})

;; SELECT id, name
;; FROM test003
;; WHERE name = $1
;; ORDER BY id ASC
;; parameters: $1 = 'Ivan'
~~~

For more options please refer to the official [HoneySQL documentation][honeysql].
