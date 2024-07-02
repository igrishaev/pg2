# Folders (Reducers)

Folders (which are also known as reducers) are objects that transform rows from
network into something new. A typical folder consists from an initial value
(which might be mutable) and logic that adds the next row to that value. Before
returning the value, the folder might post-process it somehow, for example turn
it into an immutable value.

The default folder which you don't need to specify acts exactly like this: it
spawns a new `transient` vector and `conj!` all the incoming rows into
it. Finally, it returns a `persistent!` copy of this vector.

PG2 provides a great variety of folders: to build maps or sets, to index or
group rows by a certain function. With folders, it's possible to dump a database
result into a JSON or EDN file.

It's quite important that folders process rows on the fly. Like transducers,
they don't keep the whole dataset in memory. They only track the accumulator and
the current row no matter how many rows you've got from the database: one
thousand or one million.

## A Simple Folder

Technically a folder is a function (an instance of `clojure.lang.IFn`) with
three bodies of arity 0, 1, and 2.

~~~clojure
(defn a-folder
  ([]
   ...)
  ([acc]
   ...)
  ([acc row]
   ...))
~~~

- The first 0-arity form must produce an accumulator that might be mutable.

- The third 2-arity form takes the accumulator and the current row and returns
  an updated version of the accumulator.

- The final 1-arity form takes the last version of the accumulator and
  transforms it somehow, for example turns a transient collection into its
  persistent view.

Here is the `default` folder:

~~~clojure
(defn default
  ([]
   (transient []))
  ([acc!]
   (persistent! acc!))
  ([acc! row]
   (conj! acc! row)))
~~~

Some folders depend on initial settings and thus produce folding functions. Here
is an example of the `map` folder that acts like the `map` function from
`clojure.core`:

~~~clojure
(defn map
  [f]
  (fn folder-map
    ([]
     (transient []))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (conj! acc! (f row)))))
~~~

## Passing A Folder

To pass a custom folder to process the result, specify the `:as` key as follows:

~~~clojure
(require '[pg.fold :as fold])

(defn row-sum [{:keys [field_1 field_2]}]
  (+ field_1 field_2))

(pg/execute conn query {:as (fold/map row-sum)})

;; [10 53 14 32 ...]
~~~

## Standard Folders and Aliases

PG provides a number of built-in folders. Some of them are used so often that
it's not needed to pass them explicitly via the `:as` parameter. There are
shortcuts that enable certain folders internally. Below, please find the actual
list of folders, their shortcuts and examples.

### Column

Takes a single column from each row returning a plain vector.

~~~clojure
(pg/execute conn query {:as (fold/column :id)})

;; [1 2 3 4 ....]
~~~

There is an alias `:column` that accepts the name of the column:

~~~clojure
(pg/execute conn query {:column :id})
;; [1 2 3 4 ....]
~~~

### Map

Acts like the standard `map` function from `clojure.core`. Applies a function to
each row and collects a vector of results.

Passing the folder explicitly:

~~~clojure
(pg/execute conn query {:as (fold/map func)})
~~~

And with an alias:

~~~clojure
(pg/execute conn query {:map func})
~~~

### Default

Collects unmodified rows into a vector. You barely need this folder as it gets
applied internally when no other folders have been applied.

### Dummy

A folder that doesn't accumulate the rows but just skips them always returns
nil.

~~~clojure
(pg/execute conn query {:as fold/dummy})

nil
~~~

### First

Perhaps the most needed folder, `first` tracks the first row only and skips the
rest. Pay attention that this folder doesn't have a state and thus doesn't need
to be initiated. Useful when you query a single row by its primary key:

~~~clojure
(pg/execute conn
            "select * from users where id = $1"
            {:params [42]
             :as fold/first})

{:id 42 :email "test@test.com"}
~~~

Or pass the `:first` option set to true:

~~~clojure
(pg/execute conn
            "select * from users where id = $1"
            {:params [42]
             :first true})

{:id 42 :email "test@test.com"}
~~~

### Index by

Often, you select database rows into as a vector and build a map like `{id =>
row}`, for example:

~~~clojure
(let [rows (jdbc/execute! conn ["select ..."])]
  (reduce (fn [acc row]
            (assoc acc (:id row) row))
          {}
          rows))

{1 {:id 1 :name "test1" ...}
 2 {:id 2 :name "test2" ...}
 3 {:id 3 :name "test3" ...}
 ...
 }
~~~

This process is known as indexing because later on, the map is used for quick
lookups in other functions.

This approach, although is quite common, has flaws. First, you traverse the rows
twice: when receiving them from the database, and then again inside
`reduce`. Second, it takes extra lines of code.

The `index-by` folder does exactly the same: it accepts a function which is
applied to a row and uses the result as an index key. Most often you pass a
keyword:

~~~clojure
(let [query
      "with foo (a, b) as (values (1, 2), (3, 4), (5, 6))
      select * from foo"

      res
      (pg/execute conn query {:as (fold/index-by :a)})]

{1 {:a 1 :b 2}
 3 {:a 3 :b 4}
 5 {:a 5 :b 6}})
~~~

The shortcut `:index-by` accepts a function as well:

~~~clojure
(pg/execute conn query {:index-by :a})
~~~

### Group by

The `group-by` folder is simlar to `index-by` but collects multiple rows per a
grouping function. It produces a map like `{(f row) => [row1, row2, ...]}` where
`row1`, `row2` and the rest return the same value for `f`.

Imagine each user in the database has a role:

~~~
{:id 1 :name "Test1" :role "user"}
{:id 2 :name "Test2" :role "user"}
{:id 3 :name "Test3" :role "admin"}
{:id 4 :name "Test4" :role "owner"}
{:id 5 :name "Test5" :role "admin"}
~~~

This is what `group-by` returns when grouping by the `:role` field:

~~~clojure
(pg/execute conn query {:as (fold/group-by :role)})

{"user"
 [{:id 1, :name "Test1", :role "user"}
  {:id 2, :name "Test2", :role "user"}]

 "admin"
 [{:id 3, :name "Test3", :role "admin"}
  {:id 5, :name "Test5", :role "admin"}]

 "owner"
 [{:id 4, :name "Test4", :role "owner"}]}
~~~

The folder has its own alias which accepts a function:

~~~clojure
(pg/execute conn query {:group-by :role})
~~~

### KV (Key and Value)

The `kv` folder accepts two functions: the first one is for key (`fk`), and the
second is for value (`fv`). Then it produces a map like `{(fk row) => (fv
row)}`.

A typical example might be more narrow index maps. Imagine you select just a
couple of fields, `id` and `email`. Now you need a map of `{id => email}` for
quick email lookup by id. This is where `kv` does the job for you.

~~~clojure
(pg/execute conn
            "select id, email from users"
            {:as (fold/kv :id :email)})

{1 "ivan@test.com"
 2 "hello@gmail.com"
 3 "skotobaza@mail.ru"}
~~~

The `:kv` alias accepts a vector of two functions:

~~~clojure
(pg/execute conn
            "select id, email from users"
            {:kv [:id :email]})
~~~






run
run

reduce
[f init]


into
[xf []]

to-edn
writer

to-json
writer

table
table true


java
java


## Custom Folders
