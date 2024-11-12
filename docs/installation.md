# Installation

## Core functionality

The client and the connection pool, type encoding and decoding, COPY IN/OUT,
SSL:

~~~clojure
;; lein
[com.github.igrishaev/pg2-core "0.1.21"]

;; deps
com.github.igrishaev/pg2-core {:mvn/version "0.1.21"}
~~~

## HoneySQL integration

Special version of `query` and `execute` that accept not a SQL string but a map
that gets formatted to SQL under the hood. Also includes various helpers
(`get-by-id`, `find`, `insert`, `udpate`, `delete`, etc).

~~~clojure
;; lein
[com.github.igrishaev/pg2-honey "0.1.21"]

;; deps
com.github.igrishaev/pg2-honey {:mvn/version "0.1.21"}
~~~

[component]: https://github.com/stuartsierra/component

## Component integration

A package that extends the `Connection` and `Pool` objects with the `Lifecycle`
protocol from the [Component][component] library.

~~~clojure
;; lein
[com.github.igrishaev/pg2-component "0.1.21"]

;; deps
com.github.igrishaev/pg2-component {:mvn/version "0.1.21"}
~~~

## Migrations

A package that provides migration management: migrate forward, rollback, create,
list applied migrations and so on.

~~~clojure
;; lein
[com.github.igrishaev/pg2-migration "0.1.21"]

;; deps
com.github.igrishaev/pg2-migration {:mvn/version "0.1.21"}
~~~

[hugsql]: https://www.hugsql.org/

## HugSQL support

A small wrapper on top of the well-known [HugSQL library][hugsql] which creates
Clojure functions out from SQL files.

~~~clojure
;; lein
[com.github.igrishaev/pg2-hugsql "0.1.21"]

;; deps
com.github.igrishaev/pg2-hugsql {:mvn/version "0.1.21"}
~~~
