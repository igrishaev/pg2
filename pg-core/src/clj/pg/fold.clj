(ns pg.fold
  "
  Folders: objects that reduce rows one by one
  as they come from the network.
  "
  (:import
   java.io.Writer
   java.util.ArrayList
   java.util.List
   org.pg.clojure.RowMap)
  (:require
   [pg.json :as json])
  (:refer-clojure :exclude [first
                            map
                            group-by
                            into
                            reduce]))


(defn java
  "
  Produce an ArrayList of HashMaps. Does not
  require initialization.
  "
  ([] (new ArrayList))
  ([acc] acc)
  ([^List acc ^RowMap row]
   (doto acc
     (.add (.toJavaMap row)))))


(defn column
  "
  Return a single column of the result.
  "
  [col]
  (fn folder-column
    ([]
     (transient []))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (conj! acc! (get row col)))))


(defn columns
  "
  Return certain columns of the result only.
  "
  [cols]
  (fn folder-columns
    ([]
     (transient []))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (let [values
           (persistent!
            (clojure.core/reduce
             (fn [acc! col]
               (conj! acc! (get row col)))
             (transient [])
             cols))]
       (conj! acc! values)))))


(defn map
  "
  Apply a function to reach row; collect
  the results into a persistent vector.
  "
  [f]
  (fn folder-map
    ([]
     (transient []))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (conj! acc! (f row)))))


(defn default
  "
  Collect lazy unparsed rows into a persistent
  vector.
  "
  ([]
   (transient []))
  ([acc!]
   (persistent! acc!))
  ([acc! row]
   (conj! acc! row)))


(defn dummy
  "
  Skip all the rows and return nil.
  "
  ([] nil)
  ([acc] nil)
  ([acc row] nil))


(defn first
  "
  Return the first row only.
  "
  ([] nil)
  ([acc] acc)
  ([acc row]
   (if (nil? acc)
     row
     acc)))


(defn reduce
  "
  Reduce the rows using a reducing function of two
  arguments (an accumulator and the current row)
  and an initial accumulator value.
  "
  [f init]
  (fn folder-reduce
    ([] init)
    ([acc] acc)
    ([acc row]
     (f acc row))))


(defn index-by
  "
  For one-argument function, build a map like
  {(f row), row}.
  "
  [f]
  (fn folder-index-by
    ([]
     (transient {}))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (assoc! acc! (f row) row))))


(defn group-by
  "
  For one-argument function, build a map like
  {(f row), [row row ...]}.
  "
  [f]
  (let [-conj (fnil conj [])]
    (fn folder-group-by
      ([] {})
      ([acc] acc)
      ([acc row]
       (update acc (f row) -conj row)))))


(defn kv
  "
  For a pair of one-argument functions, key and value,
  build a map like {(key row), (value row)}.
  "
  [fk fv]
  (fn folder-kv
    ([]
     (transient {}))
    ([acc!]
     (persistent! acc!))
    ([acc! row]
     (assoc! acc! (fk row) (fv row)))))


(defn run
  "
  Call a function for each row in series skipping
  the result, presumably with side effects (printing,
  writing). Return the number of total invocations.
  "
  [f]
  (fn folder-run
    ([] 0)
    ([acc] acc)
    ([acc row]
     (f row)
     (inc acc))))


(defn into
  "
  Pass rows throughout an xform like map, filter, or
  their combination. The `to` argument is a persistent
  collection which is used being transient internally.
  The items are collected using `conj!`. When `to` is
  not set, an empty vector is used.
  "
  ([xform]
   (into xform []))
  ([xform to]
   (let [f (xform conj!)]
     (fn folder-into
       ([]
        (transient to))
       ([acc!]
        (persistent! acc!))
       ([acc! row]
        (f acc! row))))))


(defn to-edn
  "
  Dump rows into an EDN writer. Expects an open `java.io.Writer`
  instance. Doesn't close it afterwards. Should be used within
  the `with-open` macro. Returns a number of rows written.
  "
  [^Writer writer]
  (fn folder-to-edn
    ([]
     (.write writer "[\n")
     0)

    ([acc]
     (.write writer "]\n")
     acc)

    ([acc row]
     (.write writer "  ")
     (.write writer (pr-str row))
     (.write writer "\n")
     (inc acc))))


(defn to-json
  "
  Dump rows into a JSON writer. Expects an open `java.io.Writer`
  instance. Doesn't close it afterwards. Should be used within
  the `with-open` macro. Returns a number of rows written.
  "
  [^Writer writer]
  (let [-sent? (volatile! false)]
    (fn folder-to-json
      ([]
       (.write writer "[\n")
       0)

      ([acc]
       (.write writer "\n]\n")
       acc)

      ([acc row]
       (if @-sent?
         (do
           (.write writer ",\n  ")
           (.write writer (json/write-string row)))
         (do
           (.write writer "  ")
           (.write writer (json/write-string row))
           (vreset! -sent? true)))
       (inc acc)))))


(defn table
  "
  Dump rows into a plain matrix with an extra header
  row which tracks names of columns.
  "
  []
  (let [-header-set? (volatile! false)]
    (fn
      ([]
       (transient []))
      ([acc!]
       (persistent! acc!))
      ([acc! ^RowMap row]
       (if @-header-set?
         (conj! acc! (.vals row))
         (do
           (vreset! -header-set? true)
           (-> acc!
               (conj! (.keys row))
               (conj! (.vals row)))))))))
