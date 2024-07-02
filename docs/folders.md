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
to be initiated:

~~~clojure
(pg/execute conn query {:as fold/first})

;; OR

(pg/execute conn query {:first true})
~~~


index-by
index-by f

group-by
group-by f


kv
[kv kv]

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
