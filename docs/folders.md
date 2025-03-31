# Folders (Reducers)

Folders (which are also known as reducers) are objects that transform rows from
network into something else. A typical folder consists from an initial value
(which might be mutable) and logic that adds the next row to that value. Before
returning the value, a folder might post-process it somehow, for example turn it
into an immutable value.

The default folder (which you don't need to specify) acts exactly like this: it
spawns a new `transient` vector and `conj!`es all the incoming rows into
it. Finally, it returns a `persistent!` version of this vector.

PG2 provides a great variety of folders: to build maps or sets, to index or
group rows by a certain function. With folders, it's possible to dump a database
result into a JSON or EDN file.

It's quite important that folders process rows on the fly. Like transducers,
they don't keep the whole dataset in memory. They only track the accumulator and
the current row no matter how many of them have arrived from the database: one
thousand or one million.

## A Simple Folder

Technically a folder is a function (an instance of `clojure.lang.IFn`) with
three bodies of arity 0, 1, and 2, as follows:

~~~clojure
(defn a-folder
  ([]
   ...)
  ([acc]
   ...)
  ([acc row]
   ...))
~~~

- The first 0-arity form produces an accumulator that might be mutable.

- The third 2-arity form takes the accumulator and the current row and returns
  an updated version of the accumulator.

- The second 1-arity form accepts the last version of the accumulator and
  transforms it somehow, for example seals a transient collection into its
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
it's not needed to pass them explicitly. There are shortcuts that enable certain
folders internally. Below, find the actual list of folders, their shortcuts and
examples.

### Column

Takes a single column from each row returning a plain vector:

~~~clojure
(pg/execute conn query {:as (fold/column :id)})

;; [1 2 3 4 ....]
~~~

There is an alias `:column` that accepts a name of the column:

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

Collects unmodified rows into a persistent vector. Each row is an instance of
the `org.pg.clojure.RowMap` class that mimics a Clojure map. The key feature is,
it holds an unparsed byte array inside and parses values lazily, meaning only
when you print a map, or access a certain key, or associate an extra key to it.

That's unlikely you'll need that folder as it gets applied internally when no
other folders were specified.

For details, check out the following section: [Working With
Rows](/docs/row-map.md).

### Dummy

A folder that doesn't accumulate the rows but just skips them and returns nil.

~~~clojure
(pg/execute conn query {:as fold/dummy})

nil
~~~

### First

Perhaps the most needed folder, `first` returns the first row only and skips the
rest. Pay attention, this folder doesn't have a state and thus doesn't need to
be initiated. Useful when you query a single row by its primary key:

~~~clojure
(pg/execute conn
            "select * from users where id = $1"
            {:params [42]
             :as fold/first})

{:id 42 :email "test@test.com"}
~~~

Or pass the `:first` (or `:first?`) option set to true:

~~~clojure
(pg/execute conn
            "select * from users where id = $1"
            {:params [42]
             :first true})

{:id 42 :email "test@test.com"}
~~~

### Index by

Often, you select rows as a vector and build a map like `{id => row}`, for
example:

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

This process is known as indexing because later on, the map is used as an index
for quick lookups.

This approach, although is quite common, has flaws. First, you traverse rows
twice: when fetching them from the database, and then again inside
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

~~~clojure
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

The `kv` folder accepts two functions: the first one is for a key (`fk`), and
the second is for a value (`fv`). Then it produces a map like `{(fk row) => (fv
row)}`.

A typical example might be a narrower index map. Imagine you select just a
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

### Run

The `run` folder is useful for processing rows with side effects, e.g. printing
them, writing to files, passing via API. A one-argument function passed to `run`
is applied to each row ignoring the result. The folder counts a total number of
rows being processed.

~~~clojure
(defn func [row]
  (println "processing row" row)
  (send-to-api row))

(pg/execute conn query {:as (fold/run func)})

100 ;; the number of rows processed
~~~

An example with an alias:

~~~clojure
(pg/execute conn query {:run func})
~~~

### Table

The `table` folder returns a plain matrix (a vector of vectors) of database
values. It reminds the `columns` folder but also keeps column names in the
leading row. Thus, the resulting table always has at least one row (it's never
empty because of the header). The table view is useful when saving the data into
CSV.

The folder has its inner state and thus needs to be initialized with no
parameters:

~~~clojure
(pg/execute conn query {:as (fold/table)})

[[:id :email]
 [1 "ivan@test.com"]
 [2 "skotobaza@mail.ru"]]
~~~

The alias `:table` accepts any non-false value:

~~~clojure
(pg/execute conn query {:table true})

[[:id :email]
 [1 "ivan@test.com"]
 [2 "skotobaza@mail.ru"]]
~~~

### Java

This folder produces `java.util.ArrayList` where each row is an instance of
`java.util.HashMap`. It doesn't require initialization:

~~~clojure
(pg/execute conn query {:as fold/java})
~~~

Alias:

~~~clojure
(pg/execute conn query {:java true})
~~~

### Reduce

The `reduce` folder acts like the same-name function from `clojure.core`. It
accepts a function and an initial value (accumulator). The function accepts the
accumulator and the current row, and returns an updated version of the
accumulator.

Here is how you collect unique pairs of size and color from the database result:

~~~clojure
(defn ->pair [acc {:keys [sku color]}]
  (conj acc [a b]))

(pg/execute conn query {:as (fold/reduce ->pair #{})})

#{[:xxl :green]
  [:xxl :red]
  [:x :red]
  [:x :blue]}
~~~

The folder ignores `reduced` logic: it performs iteration until all rows are
consumed. It doesn't check if the accumulator is wrapped with `reduced`.

The `:reduce` alias accepts a vector of a function and an initial value:

~~~clojure
(pg/execute conn query {:reduce [->pair #{}]})
~~~

### Into (Transduce)

This folder mimics the `into` logic when it deals with an `xform`, also known as
a transducer. Sometimes, you need to pass the result throughout a bunch of
`map`/`filter`/`keep` functions. Each of them produces an intermediate
collection which is not as fast as it could be with a transducer. Transducers
are designed such that they compose a stack of actions, which, when being run,
does not produce extra collections.

The `into` folder accepts an `xform` produced by `map`/`filter`/`comp`,
whatever. It also accepts a persistent collection which acts as an
accumulator. The accumulator gets transformed into a transient view internally
for better performance. The folder uses `conj!` to push values into the
accumulator, so maps are not acceptable, only vectors, lists, or sets. When the
accumulator is not passed, it's an empty vector.

Here is a quick example of `into` in action:

~~~clojure
(let [tx
      (comp (map :a)
            (filter #{1 5})
            (map str))

      query
      "with foo (a, b) as (values (1, 2), (3, 4), (5, 6))
       select * from foo"]

  (pg/execute conn query {:as (fold/into tx)}))

;; ["1" "5"]
~~~

Another case where we pass a non-empty set to collect the values:

~~~clojure
(pg/execute conn query {:as (fold/into tx #{:a :b :c})})

;; #{:a :b :c "1" "5"}
~~~

The `:into` alias is a vector where the first item is an `xform` and the second
is an accumulator:

~~~clojure
(pg/execute conn query {:into [tx []]})
~~~

### To EDN

This folder writes down rows into an EDN file. It accepts an instance of
`java.io.Writer` which must be opened in advance. The folder doesn't open nor
close the writer as these actions are beyond its scope. A common pattern is to
wrap `pg/execute` or `pg/query` invocations with the `with-open` macro that
handles closing procedure even in case of an exception.

The folder writes down rows into the writer using `pr-str`. Each row takes one
line, and the lines are split with `\n`. The leading line is `[`, and the
trailing is `]`.

The result is a number of rows processed. Here is an example of dumping rows
into a file called "test.edn":

~~~clojure
(with-open [out (-> "test.edn" io/file io/writer)]
  (pg/execute conn query {:as (fold/to-edn out)}))

;; 199
~~~

Let's check the content of the file:

~~~clojure
[
  {:id 1 :email "test@test.com"}
  {:id 2 :email "hello@test.com"}
  ...
  {:id 199 :email "ivan@test.com"}
]
~~~

The alias `:to-edn` accepts a writer object:

~~~clojure
(with-open [out (-> "test.edn" io/file io/writer)]
  (pg/execute conn query {:to-edn out}))
~~~

### To JSON

Like `to-edn` but dumps rows into JSON. Accepts an instance of
`java.io.Writer`. Writes rows line by line with no pretty printing. Lines are
joined with a comma. The leading and trailing lines are square brackets. The
result is the number of rows put into the writer.

~~~clojure
(with-open [out (-> "test.json" io/file io/writer)]
  (pg/execute conn query {:as (fold/to-json out)}))

;; 123
~~~

The content of the file:

~~~json
[
  {"b":2,"a":1},
  {"b":4,"a":3},
  // ...
  {"b":6,"a":5}
]
~~~

The `:to-json` alias accepts a writer object:

~~~clojure
(with-open [out (-> "test.json" io/file io/writer)]
  (pg/execute conn query {:to-json out}))
~~~
