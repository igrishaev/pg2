# Arrays support

In JDBC, arrays have always been a pain. Every time you're about to pass an
array to the database and read it back, you've got to wrap your data in various
Java classes, extend protocols, and multimethods. In Postgres, the array type is
quite powerful yet underestimated due to poor support of drivers. This is one
more reason for running this project: to bring easy access to Postgres arrays.

PG2 tries its best to provide seamless connection between Clojure vectors and
Postgres arrays. When reading an array, you get a Clojure vector. And vice
versa: to pass an array object into a query, just submit a vector.

PG2 supports arrays of any type: not only primitives like numbers and strings
but `uuid`, `numeric`, `timestamp(tz)`, `json(b)`, and more as well.

Arrays might have more than one dimension. Nothing prevents you from having a 3D
array of integers like `cube::int[][][]`, and it becomes a nested vector when
fetched by PG2.

*A technical note: PG2 supports both encoding and decoding of arrays in both
text and binary modes.*

Here is a short demo session. Let's prepare a table with an array of strings:

~~~clojure
(pg/query conn "create table arr_demo_1 (id serial, text_arr text[])")
~~~

Insert a simple item:

~~~clojure
(pg/execute conn
            "insert into arr_demo_1 (text_arr) values ($1)"
            {:params [["one" "two" "three"]]})
~~~

In arrays, some elements might be NULL:

~~~clojure
(pg/execute conn
            "insert into arr_demo_1 (text_arr) values ($1)"
            {:params [["foo" nil "bar"]]})
~~~

Now let's check what we've got so far:

~~~clojure
(pg/query conn "select * from arr_demo_1")

[{:id 1 :text_arr ["one" "two" "three"]}
 {:id 2 :text_arr ["foo" nil "bar"]}]
~~~

Postgres supports plenty of operators for arrays. Say, the `&&` one checks if
there is at least one common element on both sides. Here is how we find those
records that have either "tree", "four", or "five":

~~~clojure
(pg/execute conn
            "select * from arr_demo_1 where text_arr && $1"
            {:params [["three" "four" "five"]]})

[{:text_arr ["one" "two" "three"], :id 1}]
~~~

Another useful operator is `@>` that checks if the left array contains all
elements from the right array:

~~~clojure
(pg/execute conn
            "select * from arr_demo_1 where text_arr @> $1"
            {:params [["foo" "bar"]]})

[{:text_arr ["foo" nil "bar"], :id 2}]
~~~

Let's proceed with numeric two-dimensional arrays. They're widely used in math,
statistics, graphics, and similar areas:

~~~clojure
(pg/query conn "create table arr_demo_2 (id serial, matrix bigint[][])")
~~~

Here is how you insert a matrix:

~~~clojure
(pg/execute conn
            "insert into arr_demo_2 (matrix) values ($1)"
            {:params [[[[1 2] [3 4] [5 6]]
                       [[6 5] [4 3] [2 1]]]]})

{:inserted 1}
~~~

Pay attention: each number can be NULL but you cannot have NULL for an entire
sub-array. This will trigger an error response from Postgres.

Reading the matrix back:

~~~clojure
(pg/query conn "select * from arr_demo_2")

[{:id 1 :matrix [[[1 2] [3 4] [5 6]]
                 [[6 5] [4 3] [2 1]]]}]
~~~

A crazy example: let's have a three dimension array of timestamps with a time
zone. No idea how it can be used but still:

~~~clojure
(pg/query conn "create table arr_demo_3 (id serial, matrix timestamp[][][])")

(def -matrix
  [[[[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]]
   [[[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]]
   [[[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]
    [[(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]
     [(OffsetDateTime/now) (OffsetDateTime/now) (OffsetDateTime/now)]]]])

(pg/execute conn
            "insert into arr_demo_3 (matrix) values ($1)"
            {:params [-matrix]})
~~~

Now read it back:

~~~clojure
(pg/query conn "select * from arr_demo_3")

[{:matrix
  [... truncated
   [[[#object[java.time.LocalDateTime 0x5ed6e62b "2024-04-01T18:32:48.272169"]
      #object[java.time.LocalDateTime 0xb9d6851 "2024-04-01T18:32:48.272197"]
      #object[java.time.LocalDateTime 0x6e35ed84 "2024-04-01T18:32:48.272207"]]
     ...
     [#object[java.time.LocalDateTime 0x7319d217 "2024-04-01T18:32:48.272236"]
      #object[java.time.LocalDateTime 0x6153154d "2024-04-01T18:32:48.272241"]
      #object[java.time.LocalDateTime 0x2e4ffd44 "2024-04-01T18:32:48.272247"]]]
    ...
    [[#object[java.time.LocalDateTime 0x32c6e526 "2024-04-01T18:32:48.272405"]
      #object[java.time.LocalDateTime 0x496a5bc6 "2024-04-01T18:32:48.272418"]
      #object[java.time.LocalDateTime 0x283531ee "2024-04-01T18:32:48.272426"]]
     ...
     [#object[java.time.LocalDateTime 0x677b3def "2024-04-01T18:32:48.272459"]
      #object[java.time.LocalDateTime 0x46d5039f "2024-04-01T18:32:48.272467"]
      #object[java.time.LocalDateTime 0x3d0b906 "2024-04-01T18:32:48.272475"]]]]],
  :id 1}]
~~~

You can have an array of JSON(b) objects, too:

~~~clojure
(pg/query conn "create table arr_demo_4 (id serial, json_arr jsonb[])")
~~~

Inserting an array of three maps:

~~~clojure
  (pg/execute conn
              "insert into arr_demo_4 (json_arr) values ($1)"
              {:params [[{:foo 1} {:bar 2} {:test [1 2 3]}]]})
~~~

Elements might be everything that can be JSON-encoded: numbers, strings,
boolean, etc. The only tricky case is a vector. To not break the algorithm that
traverses the matrix, wrap a vector element with `pg/json-wrap`:

~~~clojure
(pg/execute conn
            "insert into arr_demo_4 (json_arr) values ($1)"
            {:params [[42 nil {:some "object"} (pg/json-wrap [1 2 3])]]})

;; Signals that the [1 2 3] is not a nested array but an element.
~~~

Now read it back:

~~~clojure
(pg/query conn "select * from arr_demo_4")

[{:id 1, :json_arr [42 nil {:some "object"} [1 2 3]]}]
~~~
