
 -> AuthenticationSASLFinal[serverFinalMessage=e=Wrong password]
 fix this case

- geom types: json, edn
- geom types: toString -> toSQL

- user & database default values?
- unix socket buffered streams

- prep stmt: track pid, conn-id
- prep stmt: check if belongs?

- postgis support
- json wrapper not needed

- fix pg11
- add pg17

- to-json: coerce output with io ns
- to-edn: coerce output with io ns

- oid: coerce long to int

- decode records
- enable binary by default?
- oid wrapper?
- copy in/out tab format
- conn init sql
- conn init prep-stmt
- close wait for 0 used conns?
- MERGE result
- UnparsedTxt, UnparsedBin types?
- test reduced?
- refactor Clojure API
- refactor Datetime enc/dec
- pg.ring package (move json middleware)
- ring jdbc session middleware
- lazy map
  - optional flag
- pg-integration package?
- docs:
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

- types
  - interval
  - hstore
  - inet/cidr

- honey helpers
  - get-by-ids-temp
  - copy in/out
  - truncate
