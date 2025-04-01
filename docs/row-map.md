# Working With Rows

When you `query` or `execute` something, by default, you get a vector of maps:

~~~clojure
(def rows
  (pg/query conn "select x from generate_series(1, 3) as x"))

rows
[{:x 1} {:x 2} {:x 3}]
~~~

These maps are not pure Clojure maps but rather special objects that mimic
Clojure maps, if fact. Namely, these are instances of the
`org.pg.clojure.RowMap` class that implements `APersistentMap` and some other
interfaces. But at first glance, it's a map indeed: get, assoc, dissoc and other
things work as expected:

~~~clojure
(-> rows first map?)
true

(-> rows first type)
org.pg.clojure.RowMap

(-> rows first (assoc :y 2))
{:y 2, :x 1}

(-> rows first :x)
1
~~~

The `RowMap` class is special in that term that it's lazy. Initially, it stores
only an array of keys and unparsed byte array that came from a Postgres
server. When you touch a certain key, a corresponding fragment of the array gets
parsed and cached. When you reach the same key the second time, it's taken from
a cache without parsing:

~~~text
{
  _keys ["Ivan" "test@test.com" ...]
  _payload [....................................]
            |---name---|-----email-----|...
  _cache {:name "Ivan"}
}

(:x row) ;; cache miss, parse, cache set
(:x row) ;; cache hit
~~~

This feature brings two positive points. First, you don't spend time on parsing
rows when receiving data from a server. Thus, the throughput is faster, and the
connection is now busy for less time. You borrow a connection, get the data
without parsing and let other threads use it again.

Second, we often select more fields that is actually needed. For example,
passing `select * from table` with 20 fields while only two of three columns are
actually needed. With laziness, all 20 fields stay unparsed until you forcibly
trigger evaluation.

The `RowMap` class stays itself only while you're reading its keys. If you
`assoc` or `dissoc` a key, it gets transformed into a Clojure map. Thus, adding
or removing a key triggers a process of parsing all keys and making a fresh
Clojure map:

~~~clojure
(-> rows first type)
org.pg.clojure.RowMap

(-> rows first (assoc :y 2) type)
clojure.lang.PersistentHashMap
~~~

A `RowMap` instance knows the order of keys which gives two features. First, it
can be used as a vector using `nth` with fast access:

~~~clojure
(nth row 0) ;; the first column
(nth row 1) ;; the second column
~~~

You can destruct a row map as a vector in `let` binding as follows:

~~~clojure
(let [[id email created-at] row]
  ...)
~~~

because under the hood, this form expands into something like this:

~~~clojure
(let [id (nth row 0)
      email (nth row 1)
      created-at (nth row 2)]
  ...)
~~~

The second feature is, it always preserves the order of columns when reading
keys, values, or processing a row as a sequence of key-value pairs. Here is a
small demo:

~~~clojure
(def row
  (pg/query conn
            "select 1 a, 2 b, 3 c, 4 d, 5 e, 6 f, 7 g, 8 h, 9 i,
                    10 j, 11 k, 12 l, 13 m, 14 n, 15 o, 16 p"
            {:first? true}))

row
{:a 1,
 :b 2,
 :c 3,
 :d 4,
 :e 5,
 :f 6,
 :g 7,
 :h 8,
 :i 9,
 :j 10,
 :k 11,
 :l 12,
 :m 13,
 :n 14,
 :o 15,
 :p 16}

(keys row)
(:a :b :c :d :e :f :g :h :i :j :k :l :m :n :o :p)

(vals row)
(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16)
~~~

Whereas pure Clojure maps don't preserve the order or keys:

~~~clojure
(def row {:a 1,
          :b 2,
          :c 3,
          ...
          :n 14,
          :o 15,
          :p 16})

(keys row)
(:o :n :m :e :l :k :g :c :j :h :b :d :f :p :i :a)
~~~

All these features make data processing a bit easier and predictable.
