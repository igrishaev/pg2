# CI (Case Insensitive) Text Support

[citext]: https://www.postgresql.org/docs/current/citext.html

"Citext" is an official extension available out from the box. It provides a
special text type called `citext` which stands for "case insensitive". When
comparing such a value with regular text, registry is not take into account.

Install the extension as follows:

~~~sql
create extension if not exists citext;
~~~

A quick demo:

~~~clojure
(pg/query conn "create temp table test (id int, email citext)")

(pg/execute conn
            "insert into test (id, email) values ($1, $2), ($3, $4), ($5, $6)"
            {:params [1 "TeSt@foo.BAR"
                      2 "hello@TEST.com"
                      3 "FoBar@Lol.kek"]})

(pg/execute conn
            "select * from test where email = $1 order by id"
            {:params ["fobar@lol.KEK"]}) ;; wrong registry

;; got the right match
;; [{:id 3, :email "FoBar@Lol.kek"}]
~~~

For details, read the official [citext documentation][hstore].
