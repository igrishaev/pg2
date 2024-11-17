
## 0.1.22-SNAPSHOT

- bump less-awful-ssl and fix pg.ssl
- point type support
- line type support
- type constructors naming
- box type support
- fromString when intput is a string

## 0.1.21

- pg.ssl namespace
- tested with neon.tech and supabase
- ssl-context param sets ssl true
- docs: ssl
- docs: services tested with

## 0.1.20

- scram plus auth #22

## 0.1.19

- fix scram branching #21

## 0.1.18

- more tests for jsonb functions
- better message render for debug
- postgis + pgvector images for docker
- docker pg11 is turned off
- pgvector support
- type processors
- bit type support
- integer OIDs
- connection init type mapping
- better tests

## 0.1.17

- nested transactions

## 0.1.16

- fix migrations name parsing #14

## 0.1.15

- better pool locking
- pool blocking queue
- tests updated
- folders/reducers refactored
- Clojure/Lazy Vectors removed
- pool/with-conn
- core/with-conn

## 0.1.14

- hugsql: def-sqlvec-fns

## 0.1.13

- refactor BIN/TXT constants
- refactor Connection constructor
- refactor Pool constructor
- pool lifetime opt renamed
- docker pg 16beta -> 16stable
- log-level removed
- connection debug
- pool logging refactored
- pool locking refactored

## 0.1.12

- pg.integration: config-txt and config-bin vars
- pg.honey: on-connection wrapper
- test json namespaces
- rename Accum to Result
- begin extended syntax (level, read only)
- column reducer
- ClojureVector type & default reducer
- lazy ToC and DataRow
- lazymap: lock passed
- read-only? conn param

## 0.1.11

- HugSQL support via pg2-hugsql package
- minor changes in readme
- future todos added

## 0.1.10

- unused imports
- KW class
- on-connection macro in pg.core
- IConnectable protocol on pg.core
- fix concurrent tests
- fix bpchar decoding

## 0.1.9

- Matrix class added
- array encode/decode bin/text support
- tests added
- JSON wrapper is idempotent
- JSON encode: default is IPersistentMap
- OID.toElementOID method
- array readme section
- todo updated

## 0.1.8

- remove type hint class
- jsonb version encoding and decoding
- bpchar encoding and decoding
- pass json(b) as a string
- refactor txt and bin encoder
- refactor server message dispatch
- remove phase enum

## 0.1.7

- migrations
  - fix file names in readme
  - fix file parser (dot vs hyphen)
  - rename files (in jar as well)

## 0.1.6

- msg package split on msg.server and msg.client
- ClientMessage and ServerMessage interfaces
- PGError and PGErrorResponse extend RuntimeException
- fixed offset/limit bug when parsing binary json
- make CodecParams class immutable
- ObjectMapper support in:
  - connection,
  - codec params,
  - pg.json ns,
  - encode/decode-bin/text,
  - JSON middleware

## 0.1.5

- migrations
- toc depth
- honey queries function

## 0.1.4

- pg-honey helpers
- pg-honey demo and reamde

## 0.1.3

- Next.JDBC wrapper
- more docs
- with-tx macro refactored
- error! function added
- msCancelTimeout renamed
- requireNonNull messages added
- connection isClosed refactored
- org.pg.proto package
- int to float encoding support
- oids type hints bug fixed
- concurrency tests

## 0.1.2

- new benchmarks
- Makefile java path
- LazyMap and LazyVector
- prg.pg.clojure package
- reducers refactored

## 0.1.1

- plan/reduce benchmarks
- reducer init() method accepts keys
- matrix header row added
- with-tx reflection warning removed

## 0.1.0

- initial release
