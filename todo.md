
- docs
  - notify & listen
  - rowmap (deref, nth)

- user & database default values?

- CodecParams
  - test toString method
  - move oid->processor map to config?

- use PG* env vars
- fix pg.server namespace

pid-sender?
pid-receiver?

- review URI config parser

poll: provide a timer?

- prep stmt: track pid, conn-id
- prep stmt: check if belongs?

- linting (kondo + clfjmt)

- ssl:
  - switch to ssl by default?
  - ssl 3rd-party tests

- geom types: json, edn
- geom types: toString -> toSQL

- read all types at startup?
  - oids -> string/keyword types?

.~/.pgpass
https://www.postgresql.org/docs/current/libpq-pgpass.html

- migrations: use PG* env vars
- migrations: unix socket
- migrations: dir path

- unix socket local tests
- ssl services local tests

- connection URI:
  - docs
  - ssl-cert-files
  - unix socket
  - logging
  - enums
  - type-map
  - connect: accept a string?

config & uri: rename pg-params to params?

- postgis support
- json wrapper not needed

- rename Result.Node to Subresult?
- test citext

- fix pg11
- add pg17

- to-json: coerce output with io ns
- to-edn: coerce output with io ns

- oid: coerce long to int

- copy in with custom types (pgvector)?

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
- bulk statement execute
- pg.jdbc batch
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

- save notifications into a list?
- get-notifications function?
