
 -> AuthenticationSASLFinal[serverFinalMessage=e=Wrong password]
 fix this case

- geom types: json, edn
- geom types: toString -> toSQL

- ssl namespace?
- doc: tested with

- readme


- postgis support
- json wrapper not needed

- fix pg11
- add pg17

- to-json: coerce output with io ns
- to-edn: coerce output with io ns

- oid: coerce long to int

- decode records
- enable binary by default?
- honey: transaction test
- oid wrapper?
- copy in/out tab format
- conn init sql
- conn init prep-stmt
- close wait for 0 used conns?
- MERGE result
- UnparsedTxt, UnparsedBin types?
- test reduced?
- PGErrorResponse: extend ExceptionInfo?
- refactor Clojure API
- refactor Datetime enc/dec
- pg.ring package (move json middleware)
- ring jdbc session middleware
- lazy map
  - optional flag
- pg-integration package?
- docs:
  - pool
  - execute parameters
  - enums
- keywords with namespaces (+ jdbc)
- test parallel connection access
- parse JDBC url
- bulk statement execute
- pg.jdbc batch
- refactor locks? on Clojure level
- async support?

migrations
- lein plugin

- malli spec

- custom types
  - encode
  - decode

- types
  - interval
  - hstore
  - inet/cidr

- Unix socket connection type

- honey helpers
  - get-by-ids-temp
  - copy in/out
  - truncate

- verify peer name? https://github.com/pgjdbc/pgjdbc/blob/5b8c2719806a9614aedeb5ee8a8b9e2b96432d28/pgjdbc/src/main/java/org/postgresql/ssl/MakeSSL.java#L30
