# Geometry types

[types]: https://www.postgresql.org/docs/current/datatype-geometric.html

Postgres provides a [number of geometry types][types] such as a line, a box, a
polygon, etc. These types, although a bit primitive, can serve simple GIS
systems where curve of the Earth doesn't count. Say, to model buildings and
routs across a city.

Recent PG2 versions suppor these types. When you read them from a database, they
arrive parsed into Clojure maps and vectors. You can pass them back as Clojure
values as well.

## Point

A point is a pair of `double` values. When read, it becomes as a map with the
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

Or as a native SQL string with parentheses:

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
                      3 "((5.003, 5.23),(9.23, -3.555))" ;; a raw SQL form
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
the initial `[{:x 0 :y 0} {:x 4 :y 6}]` pair becomes `[{:y 6.0, :x 4.0} {:y 0.0,
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

Insert three lines in various forms:

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

[{:id 1, :l {:c 3.3, :b -2.2, :a 1.1}}
 {:id 2, :l {:c -3.0, :b -2.0, :a -1.0}}
 {:id 3, :l {:c -3.555, :b 3.11, :a -9.33}}]
~~~

## Circle

A circle is a point plus its radius: `<(-1, 1), 3.3>`. In Clojure, it's either a
map with `:x`, `:y`, and `:r` keys, or a vector of three: `[x y r]`. The radius
cannot be negative.

Prepare a table:

~~~clojure
(pg/query conn "create temp table test4 (id int, c circle)")
~~~

Insert three circles in various forms:

~~~clojure
(pg/execute conn
            "insert into test4 (id, c) values ($1, $2), ($3, $4), ($5, $6)"
            {:params [1 {:x 1.1 :y -2.2 :r 3.3}
                      2 [-1 -2 3]
                      3 "<(-9.33, 3.11),3.555>"]})
~~~

The result:

~~~clojure
(pg/query conn "select * from test4")

[{:id 1, :c {:y -2.2, :r 3.3, :x 1.1}}
 {:id 2, :c {:y -2.0, :r 3.0, :x -1.0}}
 {:id 3, :c {:y 3.11, :r 3.555, :x -9.33}}]
~~~

## Polygon

A polygon is a collection of points surrounded with parentheses:
`((1.1,-2.2),(3.3,4.4),(-5.5,6.6))`. A table:

~~~clojure
(pg/query conn "create temp table test5 (id int, poly polygon)")
~~~

Let's insert polygons. Pay attention that points may follow in various forms: a
map, a vector, then a map again:

~~~clojure
(pg/execute conn
            "insert into test5 (id, poly) values ($1, $2), ($3, $4), ($5, $6)"
            {:params [1 [[1 2] {:x 3 :y 4} [5 6]]
                      2 [{:x 8 :y 3} {:x 5 :y -5}]
                      3 "((1,2),(3,4),(5,6),(7,8))"]})
~~~

The result:

~~~clojure
(pg/query conn "select * from test5")

[{:id 1
  :poly [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0} {:y 6.0, :x 5.0}]}
 {:id 2
  :poly [{:y 3.0, :x 8.0} {:y -5.0, :x 5.0}]}
 {:id 3
  :poly [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0} {:y 6.0, :x 5.0} {:y 8.0, :x 7.0}]}]
~~~

## Line segment

A line segment (the `lseg` type) is just a pair of points: `[(1,2),(3,4)]`. A
table:

~~~clojure
(pg/query conn "create temp table test6 (id int, seg lseg)")
~~~

Insertion:

~~~clojure
(pg/execute conn
            "insert into test6 (id, seg) values ($1, $2), ($3, $4), ($5, $6)"
            {:params [1 [[1 2] {:x 3 :y 4}]
                      2 [{:x 8 :y 3} {:x 5 :y -5}]
                      3 "((1,2),(3,4))"]})
~~~
The result:

~~~clojure
(pg/execute conn "select * from test6")

[{:id 1, :seg [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}]}
 {:id 2, :seg [{:y 3.0, :x 8.0} {:y -5.0, :x 5.0}]}
 {:id 3, :seg [{:y 2.0, :x 1.0} {:y 4.0, :x 3.0}]}]
 ~~~

## Path

Paths are a bit tricky. Technically they are collections of points with an
additional flag. A path might be either **open** (meaning there is no a
connection between the first and the last points) and **closed** (meaning there
is). Square brackets `[]` indicate an open path, while parentheses `()` indicate
a closed path.

That extra flag slightly complicates Clojure representation. A path is no longer
a vector of points but a map with the `:points` and `:closed?` flag. When the
flag is not set, the path is considered as **closed**.

Prepare a table:

~~~clojure
(pg/query conn "create temp table test7 (id int, p path)")
~~~

Passing multiple paths in various forms:

~~~clojure
(pg/execute conn
            "insert into test7 (id, p) values ($1, $2), ($3, $4), ($5, $6), ($7, $8), ($9, $10)"
            {:params [1 [[1 -2] {:x 3 :y 4}]            ;; a vector of points, closed
                      2 {:closed? true                  ;; a map form, closed
                         :points [[1 2] {:x 3 :y 4}]}
                      3 {:closed? false                 ;; a map form, open
                         :points [[1 2] {:x 3 :y 4}]}
                      4 "((1,2),(3,4))"                 ;; SQL form, closed
                      5 "[(5,4),(3,2)]"                 ;; SQL form, open
            ]})
~~~

The result:

~~~clojure
(pg/execute conn "select * from test7 order by id")

[{:id 1, :p {:points [{:y -2.0, :x 1.0} {:y 4.0, :x 3.0}], :closed? true}}
 {:id 2, :p {:points [{:y  2.0, :x 1.0} {:y 4.0, :x 3.0}], :closed? true}}
 {:id 3, :p {:points [{:y  2.0, :x 1.0} {:y 4.0, :x 3.0}], :closed? false}}
 {:id 4, :p {:points [{:y  2.0, :x 1.0} {:y 4.0, :x 3.0}], :closed? true}}
 {:id 5, :p {:points [{:y  4.0, :x 5.0} {:y 2.0, :x 3.0}], :closed? false}}]
 ~~~
