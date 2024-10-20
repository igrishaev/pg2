# Installation

## Core functionality

The client and the connection pool, type encoding and decoding, `COPY IN/OUT`,
SSL:

~~~clojure
;; lein
[com.github.igrishaev/pg2-core "0.1.17"]

;; deps.edn
com.github.igrishaev/pg2-core {:mvn/version "0.1.17"}
~~~

## HoneySQL Integration

Special version of `query` and `execute` that accept `HoneySQL` map.
Also includes helpers like `get-by-id`, `find`, `insert`, `update`, `delete`, etc.

~~~clojure
;; lein
[com.github.igrishaev/pg2-honey "0.1.17"]

;; deps.edn
com.github.igrishaev/pg2-honey {:mvn/version "0.1.17"}
~~~

[component]: https://github.com/stuartsierra/component

## Component Integration

Extends `Connection` and `Pool` objects with `Lifecycle`
protocol from [Component][component].

~~~clojure
;; lein
[com.github.igrishaev/pg2-component "0.1.17"]

;; deps.edn
com.github.igrishaev/pg2-component {:mvn/version "0.1.17"}
~~~

## Migrations

Migrations management: migrate forward, rollback, create, list applied migrations, etc.

~~~clojure
;; lein
[com.github.igrishaev/pg2-migration "0.1.17]

;; deps.edn
com.github.igrishaev/pg2-migration {:mvn/version "0.1.17"}
~~~

[hugsql]: https://www.hugsql.org/

## HugSQL Integration

Wrapper for [HugSQL][hugsql].

~~~clojure
;; lein
[com.github.igrishaev/pg2-hugsql "0.1.17]

;; deps.edn
com.github.igrishaev/pg2-hugsql {:mvn/version "0.1.17"}
~~~
