# Geometry types

[types]: https://www.postgresql.org/docs/current/datatype-geometric.html

Postgres provides a [number of geometry types][types] such as line, box,
polygon, etc. These types, although a bit primitive, can be used to build simple
GIS systems where curve of the Earth doesn't count. Say, to model buildings and
routs across a city.

Recent PG2 versions suppors these types: when you read them from a database,
they arrive being parsed into Clojure maps and vectors. You can pass them back
as Clojure values as well.

## Point

A point is a pair of `double` values. When read, it appears as a map with the
`:x` and `:y` keys:

~~~clojure
(pg/query conn "select '(1.01, -2.003)'::point as point")

[{:point {:x 1.01 :y -2.003}}]
~~~

Inserting a point:

~~~clojure
(pg/query conn "create temp table test1 (id int, p point)")

(pg/execute conn
            "insert into test1 (id, p) values ($1, $2)"
            {:params [1 {:x 2 :y 3}]})
~~~

You can pass it as a vector as well:

~~~clojure
(pg/execute conn
            "insert into test1 (id, p) values ($1, $2)"
            {:params [2 [-3, 6.003]]})
~~~

Or as a native SQL string:

~~~clojure
(pg/execute conn
            "insert into test1 (id, p) values ($1, $2)"
            {:params [3 "(3.003, -42.01)"]})
~~~

Let's check out the table:

~~~clojure
(pg/query conn "select * from test1")

[{:id 1, :p {:y 3.0, :x 2.0}}
 {:id 2, :p {:y 6.003, :x -3.0}}
 {:id 3, :p {:y -42.01, :x 3.003}}]
~~~

## Box

A box is a pair of points representing its corners, e.g. `((x1, y1), (x2,
y2))`. A test table:

~~~clojure
(pg/query conn "create temp table test2 (id int, b box)")
~~~

Let's insert three boxes at once using various forms:

~~~clojure
(pg/execute conn
            "insert into test2 (id, b) values ($1, $2), ($3, $4), ($5, $6)"
            {:params [1 [{:x 0 :y 0} {:x 4 :y 6}]        ;; a vector of maps
                      2 [[-1 3] [6.003 8.33]]            ;; a vector of pairs
                      3 "((5.003, 5.23),(9.23, -3.555))" ;; raw SQL form
                      ]})
~~~

The result:

~~~clojure
(pg/execute conn "select * from test2")

[{:id 1, :b [{:y 6.0, :x 4.0} {:y 0.0, :x 0.0}]}
 {:id 2, :b [{:y 8.33, :x 6.003} {:y 3.0, :x -1.0}]}
 {:id 3, :b [{:y 5.23, :x 9.23} {:y -3.555, :x 5.003}]}]
~~~

Pay attention that Postgres normalizes box's corners. When reading boxes back,
the initial `[{:x 0 :y 0} {:x 4 :y 6}]` form becomes `[{:y 6.0, :x 4.0} {:y 0.0,
:x 0.0}]` (the points where swapped).

## Line

As the docs say:

> Lines are represented by the linear equation Ax + By + C = 0, where A and B
> are not both zero. Values of type line are input and output in the following
> form: { A, B, C }

In PG2, a line is either a map with `:a`, `:b`, and `:c` keys, or a vector of
three `double` values. Prepare a table:

~~~clojure
(pg/query conn "create temp table test3 (id int, l line)")
~~~

Insert in three various forms:

~~~clojure
(pg/execute conn
            "insert into test3 (id, l) values ($1, $2), ($3, $4), ($5, $6)"
            {:params [1 {:a 1.1 :b -2.2 :c 3.3}
                      2 [-1 -2 -3]
                      3 "{-9.33, 3.11, -3.555}"]})

~~~

The result:

~~~clojure
(pg/execute conn "select * from test3")

[{:l {:c 3.3, :b -2.2, :a 1.1}, :id 1}
 {:l {:c -3.0, :b -2.0, :a -1.0}, :id 2}
 {:l {:c -3.555, :b 3.11, :a -9.33}, :id 3}]
~~~

## Circle
