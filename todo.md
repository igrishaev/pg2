
- readme

- oid hints
  (pg/execute conn
              "insert into test_json (data) values ($1)"
              {:params ["[1, 2, 3]"]})
  - json string
  - honeysql prepare params


- encode: reverse type->oid to oid->type?

- json
  - ring middleware object mapper
  - object-mapper: change order?
  - readme

- pool close conn test
- keywords with namespaces (+ jdbc)
- test parallel connection access
- migration: no ragtime?
- parse JDBC url
- bulk statement execute
- batch execute
- pg.jdbc batch
- pass jsonista custom object

- arrays
  - decode txt
  - decode bin
  - encode txt
  - encode bin

- int to float: tests
- refactor locks? on Clojure level

migrations
- lein plugin

- malli spec

- custom types
  - encode
  - decode

- types
  - interval
  - hstore
  - geom.box
  - geom.line
  - geom.point
  - geom.polygon
  - geom.etc
  - inet/cidr

- use COPY OUT for result?

- Unix socket connection type

- hugsql adapter

- reduce java version by converting records to classes

- honey helpers
  - get-by-ids-temp
  - copy in/out
  - truncate

- verify peer name? https://github.com/pgjdbc/pgjdbc/blob/5b8c2719806a9614aedeb5ee8a8b9e2b96432d28/pgjdbc/src/main/java/org/postgresql/ssl/MakeSSL.java#L30
